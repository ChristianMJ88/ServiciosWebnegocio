import { Injectable } from '@angular/core';

interface FacebookSdk {
  init(config: Record<string, unknown>): void;
  login(callback: (response: unknown) => void, options: Record<string, unknown>): void;
}

declare global {
  interface Window {
    FB?: FacebookSdk;
    fbAsyncInit?: () => void;
  }
}

export interface MetaEmbeddedSignupConfig {
  appId: string;
  configurationId: string;
  partnerSolutionId: string;
}

export interface MetaEmbeddedSignupResult {
  wabaId: string;
  phoneNumberId: string | null;
}

@Injectable({ providedIn: 'root' })
export class MetaEmbeddedSignupService {
  private sdkPromise: Promise<void> | null = null;

  async abrir(config: MetaEmbeddedSignupConfig): Promise<MetaEmbeddedSignupResult> {
    await this.cargarSdk(config.appId);
    if (!window.FB) throw new Error('No fue posible cargar Facebook Login for Business.');

    return new Promise<MetaEmbeddedSignupResult>((resolve, reject) => {
      let terminado = false;
      const finalizar = (accion: () => void) => {
        if (terminado) return;
        terminado = true;
        window.removeEventListener('message', escuchar);
        accion();
      };
      const escuchar = (event: MessageEvent) => {
        if (!this.esOrigenFacebook(event.origin)) return;
        try {
          const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
          if (data?.type !== 'WA_EMBEDDED_SIGNUP') return;
          if (data.event === 'FINISH') {
            const wabaId = data.data?.waba_id;
            if (!wabaId) return finalizar(() => reject(new Error('Meta no devolvió el WABA ID.')));
            finalizar(() => resolve({
              wabaId,
              phoneNumberId: data.data?.phone_number_id ?? null
            }));
          } else if (data.event === 'CANCEL') {
            finalizar(() => reject(new Error('La conexión con Meta fue cancelada.')));
          } else if (data.event === 'ERROR') {
            finalizar(() => reject(new Error(data.data?.error_message || 'Meta no pudo completar la conexión.')));
          }
        } catch {
          // Facebook también emite mensajes que no pertenecen a Embedded Signup.
        }
      };

      window.addEventListener('message', escuchar);
      window.FB!.login(() => undefined, {
        config_id: config.configurationId,
        auth_type: 'rerequest',
        response_type: 'code',
        override_default_response_type: true,
        extras: { setup: { solutionID: config.partnerSolutionId } }
      });
    });
  }

  private cargarSdk(appId: string): Promise<void> {
    if (window.FB) {
      window.FB.init({ appId, autoLogAppEvents: true, xfbml: false, version: 'v21.0' });
      return Promise.resolve();
    }
    if (this.sdkPromise) return this.sdkPromise;

    this.sdkPromise = new Promise<void>((resolve, reject) => {
      window.fbAsyncInit = () => {
        window.FB?.init({ appId, autoLogAppEvents: true, xfbml: false, version: 'v21.0' });
        resolve();
      };
      const script = document.createElement('script');
      script.async = true;
      script.defer = true;
      script.crossOrigin = 'anonymous';
      script.src = 'https://connect.facebook.net/es_LA/sdk.js';
      script.onerror = () => reject(new Error('No fue posible descargar el SDK de Meta.'));
      document.head.appendChild(script);
    });
    return this.sdkPromise;
  }

  private esOrigenFacebook(origin: string): boolean {
    try {
      const host = new URL(origin).hostname;
      return host === 'facebook.com' || host.endsWith('.facebook.com');
    } catch {
      return false;
    }
  }
}
