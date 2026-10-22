import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TenantSiteConfig } from './tenant.types';

@Injectable({
  providedIn: 'root'
})
export class TenantSiteService {
  private readonly http = inject(HttpClient);
  private hostInicialConsumido = false;

  getBySlug(slug: string): Observable<TenantSiteConfig> {
    const slugNormalizado = slug.trim().toLowerCase();
    if (!slugNormalizado) {
      return throwError(() => new Error('Se requiere el slug del tenant'));
    }

    const tenantInyectado = this.tenantInyectado();
    if (tenantInyectado?.slug?.trim().toLowerCase() === slugNormalizado) {
      return of(tenantInyectado);
    }

    if (!environment.apiBaseUrl) {
      return throwError(() => new Error('La API pública no está configurada'));
    }

    return this.http.get<TenantSiteConfig>(
      `${environment.apiBaseUrl}/publico/sitio/${encodeURIComponent(slugNormalizado)}`
    );
  }

  private tenantInyectado(): TenantSiteConfig | null {
    const runtime = globalThis as typeof globalThis & {
      __FLUORA_TENANT__?: TenantSiteConfig;
    };
    return runtime.__FLUORA_TENANT__ ?? null;
  }

  getByHostname(hostname: string): Observable<TenantSiteConfig> {
    const hostNormalizado = hostname.trim().toLowerCase();
    if (!hostNormalizado) {
      return throwError(() => new Error('Se requiere el dominio del tenant'));
    }

    const runtime = globalThis as typeof globalThis & { __FLUORA_CUSTOM_HOST__?: string; __FLUORA_TENANT__?: TenantSiteConfig };
    if (!this.hostInicialConsumido && runtime.__FLUORA_CUSTOM_HOST__ === hostNormalizado && runtime.__FLUORA_TENANT__) {
      this.hostInicialConsumido = true;
      const tenant = runtime.__FLUORA_TENANT__;
      delete runtime.__FLUORA_CUSTOM_HOST__;
      delete runtime.__FLUORA_TENANT__;
      return of(tenant);
    }

    if (!environment.apiBaseUrl) {
      return throwError(() => new Error('La API pública no está configurada'));
    }

    return this.http.get<TenantSiteConfig>(
      `${environment.apiBaseUrl}/publico/sitio/host/${encodeURIComponent(hostNormalizado)}`
    );
  }
}
