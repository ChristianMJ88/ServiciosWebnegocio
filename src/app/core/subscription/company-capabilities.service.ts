import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CompanyModuleCapability {
  codigo: string;
  nombre: string;
  limiteUso: number | null;
}

export interface CompanyLimitCapability {
  codigo: string;
  nombre: string;
  unidad: string;
  valor: number;
}

export interface CompanyCapabilities {
  suscripcionVigente: boolean;
  suscripcionId: number | null;
  planId: number | null;
  planCodigo: string | null;
  planNombre: string | null;
  estado: string;
  pruebaHasta: string | null;
  periodoActualHasta: string | null;
  modulos: CompanyModuleCapability[];
  limites: CompanyLimitCapability[];
  seccionesAdmin: string[];
}

@Injectable({ providedIn: 'root' })
export class CompanyCapabilitiesService {
  private readonly http = inject(HttpClient);
  private readonly capabilities = signal<CompanyCapabilities | null>(null);
  private loadingRequest$: Observable<CompanyCapabilities> | null = null;

  readonly current = computed(() => this.capabilities());
  readonly adminSectionCodes = computed(() => new Set(this.capabilities()?.seccionesAdmin ?? []));

  load(force = false): Observable<CompanyCapabilities> {
    if (!force && this.loadingRequest$) return this.loadingRequest$;
    const request$ = this.http.get<CompanyCapabilities>(`${environment.apiBaseUrl}/admin/suscripcion/capacidades`).pipe(
      tap(capabilities => this.capabilities.set(capabilities)),
      finalize(() => this.loadingRequest$ = null),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    this.loadingRequest$ = request$;
    return request$;
  }

  hasAdminSection(code: string): boolean {
    return this.adminSectionCodes().has(code);
  }

  limit(code: string): number | null {
    return this.capabilities()?.limites.find(limit => limit.codigo === code)?.valor ?? null;
  }

  clear(): void {
    this.capabilities.set(null);
    this.loadingRequest$ = null;
  }
}
