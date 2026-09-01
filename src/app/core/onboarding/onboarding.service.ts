import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, concat, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RespuestaAccesoApp, RespuestaTokenJwt } from '../auth/auth.service';

export interface RegistrarEmpresaPayload {
  nombreEmpresa: string;
  slug?: string;
  giro: string;
  tamanoEquipo: string;
  nombreAdministrador: string;
  correoAdministrador: string;
  telefonoAdministrador: string;
  contrasena?: string;
  zonaHoraria?: string;
  registroSocialToken?: string;
}

export interface ConfiguracionRegistroSocial {
  google: boolean;
  microsoft: boolean;
  apple: boolean;
  googleInicioUrl: string | null;
  googleAccesoUrl: string | null;
  microsoftInicioUrl: string | null;
  microsoftAccesoUrl: string | null;
}

export interface PerfilRegistroSocial {
  proveedor: string;
  correo: string;
  nombre: string;
}

export interface RegistrarEmpresaResponse {
  empresaId: number;
  slug: string;
  nombreEmpresa: string;
  correoAdministrador: string;
  rutaSitioPublico: string;
  rutaAcceso: string;
  requiereConfirmacionCorreo: boolean;
  sesion: RespuestaTokenJwt;
}

export interface EstadoOnboarding {
  correoVerificado: boolean;
  correo: string;
  categoria: string;
  tamanoEquipo: string;
  pasoRecomendado: string;
  pasosCompletados: number;
  totalPasos: number;
  pasosPendientes: string[];
}

export interface ConfirmarCorreoResponse { mensaje: string; rutaAcceso: string; }

@Injectable({
  providedIn: 'root'
})
export class OnboardingService {
  private readonly http = inject(HttpClient);
  private readonly socialConfigStorageKey = 'fluora_social_config';
  private socialConfigRequest?: Observable<ConfiguracionRegistroSocial>;

  registrarEmpresa(payload: RegistrarEmpresaPayload): Observable<RegistrarEmpresaResponse> {
    if (!environment.apiBaseUrl) {
      return throwError(() => new Error('El onboarding todavía no está disponible en este entorno.'));
    }

    return this.http.post<RegistrarEmpresaResponse>(`${environment.apiBaseUrl}/onboarding/empresas`, payload);
  }

  configuracionRegistroSocial(): Observable<ConfiguracionRegistroSocial> {
    if (this.socialConfigRequest) return this.socialConfigRequest;

    const cache = this.leerConfiguracionSocialCache();
    const remota = this.http.get<ConfiguracionRegistroSocial>(`${environment.apiBaseUrl}/auth/social/configuracion`).pipe(
      tap(config => this.guardarConfiguracionSocialCache(config)),
      catchError(error => cache ? of(cache) : throwError(() => error)),
      shareReplay({bufferSize: 1, refCount: false})
    );
    this.socialConfigRequest = cache ? concat(of(cache), remota) : remota;
    return this.socialConfigRequest;
  }

  private leerConfiguracionSocialCache(): ConfiguracionRegistroSocial | null {
    try {
      const valor = globalThis.localStorage?.getItem(this.socialConfigStorageKey);
      if (!valor) return null;
      const config = JSON.parse(valor) as ConfiguracionRegistroSocial;
      return typeof config.google === 'boolean' && typeof config.microsoft === 'boolean' ? config : null;
    } catch {
      return null;
    }
  }

  private guardarConfiguracionSocialCache(config: ConfiguracionRegistroSocial): void {
    try {
      globalThis.localStorage?.setItem(this.socialConfigStorageKey, JSON.stringify(config));
    } catch {
      // El acceso sigue funcionando aunque el navegador no permita almacenamiento local.
    }
  }

  perfilRegistroSocial(token: string): Observable<PerfilRegistroSocial> {
    return this.http.post<PerfilRegistroSocial>(`${environment.apiBaseUrl}/auth/social/perfil-registro`, {token});
  }

  urlInicioSocial(ruta: string): string {
    return `${environment.apiBaseUrl}${ruta}`;
  }

  confirmarCorreo(token: string): Observable<ConfirmarCorreoResponse> {
    return this.http.post<ConfirmarCorreoResponse>(`${environment.apiBaseUrl}/publico/onboarding/verificacion-correo/confirmar`, {token});
  }

  estado(): Observable<EstadoOnboarding> {
    return this.http.get<EstadoOnboarding>(`${environment.apiBaseUrl}/onboarding/estado`);
  }

  reenviarConfirmacion(): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/onboarding/estado/reenviar-confirmacion`, {});
  }

  intercambiarAccesoSocial(codigo: string, empresaId?: number): Observable<RespuestaAccesoApp> {
    return this.http.post<RespuestaAccesoApp>(`${environment.apiBaseUrl}/auth/social/intercambiar-acceso`, {codigo, empresaId});
  }
}
