import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { InjectionToken, PLATFORM_ID, Provider, Signal, inject, signal } from '@angular/core';
import { ResolveFn } from '@angular/router';

export interface TenantPublicStyles {
  readonly ready: Signal<boolean>;
  ensure(): Promise<void>;
}

/** Application already loads its complete CSS; tests keep that default. */
export const TENANT_PUBLIC_STYLES = new InjectionToken<TenantPublicStyles>('Tenant public styles', {
  providedIn: 'root',
  factory: () => ({ ready: signal(true).asReadonly(), ensure: () => Promise.resolve() }),
});

export function createTenantPublicStyles(document: Document, timeoutMs = 10_000): TenantPublicStyles {
  const ready = signal(false);
  let pending: Promise<void> | null = null;
  let loaded = false;
  return {
    ready: ready.asReadonly(),
    ensure() {
      if (loaded) return Promise.resolve();
      if (pending) return pending;
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = new URL('tenant-public.css', document.baseURI).href;
      link.dataset['tenantPublicStyles'] = '';
      pending = new Promise<void>(resolve => {
        const timeout = setTimeout(() => {
          // Retain the same request: late CSS still applies without a second link.
          ready.set(true); resolve();
          console.error('La carga de estilos públicos del negocio está tardando más de lo esperado.');
        }, timeoutMs);
        link.onload = () => { clearTimeout(timeout); loaded = true; ready.set(true); resolve(); };
        link.onerror = () => {
          clearTimeout(timeout);
          link.remove(); pending = null;
          // Fail open like a normal global CSS request, without hanging routing;
          // the next navigation/ensure can retry. No business decision here.
          ready.set(true);
          console.error('No se pudieron cargar los estilos públicos del negocio.');
          resolve();
        };
      });
      const global = document.head.querySelector<HTMLLinkElement>('link[rel="stylesheet"][href^="styles"]');
      const componentStyle = [...document.head.querySelectorAll('style')].find(style => /_ng(?:content|host)-/.test(style.textContent || '')) ?? null;
      if (!global) console.error('No se encontró la hoja global esperada; se conserva el CSS crítico antes del CSS del negocio.');
      const before = global ? global.nextSibling : componentStyle;
      document.head.insertBefore(link, before);
      return pending;
    },
  };
}

export function provideTenantPublicStyles(): Provider {
  return { provide: TENANT_PUBLIC_STYLES, useFactory: () => isPlatformBrowser(inject(PLATFORM_ID))
    ? createTenantPublicStyles(inject(DOCUMENT))
    : {ready:signal(true).asReadonly(),ensure:()=>Promise.resolve()} };
}

export const tenantPublicStylesResolver: ResolveFn<true> = () =>
  inject(TENANT_PUBLIC_STYLES).ensure().then(() => true as const);
