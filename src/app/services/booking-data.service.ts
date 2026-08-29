import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { TenantContextService } from '../core/tenant/tenant-context.service';

export interface SucursalCatalogo {
  id: number;
  empresaId: number;
  nombre: string;
  direccion: string;
  telefono: string;
  zonaHoraria: string;
}

export interface ServicioCatalogo {
  id: number;
  sucursalId: number;
  grupoId: number | null;
  grupoNombre: string | null;
  subgrupoId: number | null;
  subgrupoNombre: string | null;
  nombre: string;
  slug: string | null;
  descripcion: string;
  imagenUrl: string | null;
  duracionMinutos: number;
  bufferAntesMinutos: number;
  bufferDespuesMinutos: number;
  precio: number;
  moneda: string;
  ordenPublico: number;
  visiblePublico: boolean;
  requiereAnticipo: boolean;
  anticipoTipo: string | null;
  anticipoValor: number | null;
}

export interface SubgrupoCatalogoPublico {
  id: number;
  nombre: string;
  descripcion: string | null;
  ordenPublico: number;
  servicios: ServicioCatalogo[];
}

export interface GrupoCatalogoPublico {
  id: number;
  nombre: string;
  descripcion: string | null;
  imagenUrl: string | null;
  icono: string | null;
  ordenPublico: number;
  subgrupos: SubgrupoCatalogoPublico[];
}

export interface CatalogoServiciosPublico {
  empresaId: number;
  slug: string;
  nombreComercial: string;
  grupos: GrupoCatalogoPublico[];
}

@Injectable({
  providedIn: 'root'
})
export class BookingDataService {
  private readonly tenantContext = inject(TenantContextService);

  constructor(private http: HttpClient) {}

  getBranches(empresaId?: number | null): Observable<SucursalCatalogo[]> {
    const empresaObjetivo = empresaId ?? this.tenantContext.empresaId();
    if (!empresaObjetivo) {
      return throwError(() => new Error('No hay un tenant activo para consultar sucursales'));
    }
    return this.http.get<SucursalCatalogo[]>(
      `${environment.apiBaseUrl}/publico/sucursales?empresaId=${empresaObjetivo}`
    );
  }

  getServices(branchId: number): Observable<ServicioCatalogo[]> {
    return this.http.get<ServicioCatalogo[]>(
      `${environment.apiBaseUrl}/publico/servicios?sucursalId=${branchId}`
    );
  }

  getPublicCatalogBySlug(slug: string): Observable<CatalogoServiciosPublico> {
    const slugNormalizado = slug.trim().toLowerCase();
    if (!slugNormalizado) {
      return throwError(() => new Error('Se requiere el slug para consultar el catálogo'));
    }
    return this.http.get<CatalogoServiciosPublico>(
      `${environment.apiBaseUrl}/publico/servicios/sitio/${encodeURIComponent(slugNormalizado)}`
    );
  }
}
