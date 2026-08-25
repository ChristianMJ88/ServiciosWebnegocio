import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
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
  private readonly LEGACY_BRANCHES: SucursalCatalogo[] = [
    {
      id: 1,
      empresaId: environment.empresaId,
      nombre: 'Sucursal Principal',
      direccion: 'Por definir',
      telefono: '5550000000',
      zonaHoraria: 'America/Mexico_City'
    }
  ];

  private readonly LEGACY_SERVICES: ServicioCatalogo[] = [
    {
      id: 1,
      sucursalId: 1,
      grupoId: 1,
      grupoNombre: 'Uñas',
      subgrupoId: 1,
      subgrupoNombre: 'Manicura',
      nombre: 'Manicura',
      slug: 'manicura',
      descripcion: 'Servicio clasico',
      imagenUrl: null,
      duracionMinutos: 60,
      bufferAntesMinutos: 0,
      bufferDespuesMinutos: 10,
      precio: 250,
      moneda: 'MXN',
      ordenPublico: 10,
      visiblePublico: true,
      requiereAnticipo: false,
      anticipoTipo: null,
      anticipoValor: null
    },
    {
      id: 2,
      sucursalId: 1,
      grupoId: 1,
      grupoNombre: 'Uñas',
      subgrupoId: 2,
      subgrupoNombre: 'Pedicura',
      nombre: 'Pedicura',
      slug: 'pedicura',
      descripcion: 'Servicio clasico',
      imagenUrl: null,
      duracionMinutos: 75,
      bufferAntesMinutos: 0,
      bufferDespuesMinutos: 15,
      precio: 320,
      moneda: 'MXN',
      ordenPublico: 20,
      visiblePublico: true,
      requiereAnticipo: false,
      anticipoTipo: null,
      anticipoValor: null
    },
    {
      id: 3,
      sucursalId: 1,
      grupoId: 1,
      grupoNombre: 'Uñas',
      subgrupoId: 3,
      subgrupoNombre: 'Extensiones',
      nombre: 'Uñas Acrílicas',
      slug: 'unas-acrilicas',
      descripcion: 'Servicio especial',
      imagenUrl: null,
      duracionMinutos: 90,
      bufferAntesMinutos: 0,
      bufferDespuesMinutos: 15,
      precio: 450,
      moneda: 'MXN',
      ordenPublico: 30,
      visiblePublico: true,
      requiereAnticipo: true,
      anticipoTipo: 'FIJO',
      anticipoValor: 100
    }
  ];

  constructor(private http: HttpClient) {}

  getBranches(empresaId?: number | null): Observable<SucursalCatalogo[]> {
    const empresaObjetivo = empresaId ?? this.tenantContext.empresaId() ?? environment.empresaId;
    if (environment.useBackendCatalog && environment.apiBaseUrl) {
      return this.http.get<SucursalCatalogo[]>(
        `${environment.apiBaseUrl}/publico/sucursales?empresaId=${empresaObjetivo}`
      ).pipe(
        catchError(error => {
          if (environment.allowLegacyFallback) {
            console.warn('Fallo el catálogo backend, usando fallback legacy.', error);
            return of(
              [...this.LEGACY_BRANCHES].map(branch => ({
                ...branch,
                empresaId: empresaObjetivo
              }))
            );
          }
          throw error;
        })
      );
    }

    return of(
      [...this.LEGACY_BRANCHES].map(branch => ({
        ...branch,
        empresaId: empresaObjetivo
      }))
    );
  }

  getServices(branchId: number): Observable<ServicioCatalogo[]> {
    if (environment.useBackendCatalog && environment.apiBaseUrl) {
      return this.http.get<ServicioCatalogo[]>(
        `${environment.apiBaseUrl}/publico/servicios?sucursalId=${branchId}`
      ).pipe(
        catchError(error => {
          if (environment.allowLegacyFallback) {
            console.warn('Fallo el catálogo backend, usando servicios legacy.', error);
            return of(this.LEGACY_SERVICES.filter(service => service.sucursalId === branchId));
          }
          throw error;
        })
      );
    }

    return of(this.LEGACY_SERVICES.filter(service => service.sucursalId === branchId));
  }

  getPublicCatalogBySlug(slug: string): Observable<CatalogoServiciosPublico> {
    const slugNormalizado = slug.trim().toLowerCase();
    if (environment.apiBaseUrl) {
      return this.http.get<CatalogoServiciosPublico>(
        `${environment.apiBaseUrl}/publico/servicios/sitio/${slugNormalizado}`
      ).pipe(
        catchError(error => {
          if (environment.allowLegacyFallback) {
            console.warn('Fallo el catálogo público backend, usando fallback legacy.', error);
            return of(this.buildLegacyCatalog(slugNormalizado));
          }
          throw error;
        })
      );
    }

    return of(this.buildLegacyCatalog(slugNormalizado));
  }

  private buildLegacyCatalog(slug: string): CatalogoServiciosPublico {
    return {
      empresaId: this.tenantContext.empresaId() ?? environment.empresaId,
      slug: slug || this.tenantContext.slug() || 'demo',
      nombreComercial: this.tenantContext.nombreComercial(),
      grupos: [
        {
          id: 1,
          nombre: 'Uñas',
          descripcion: 'Servicios principales para manos y pies.',
          imagenUrl: '/NailArt_logo.jpeg',
          icono: 'spa',
          ordenPublico: 10,
          subgrupos: [
            {
              id: 1,
              nombre: 'Manicura',
              descripcion: 'Clásica y gel.',
              ordenPublico: 10,
              servicios: this.LEGACY_SERVICES.filter(service => service.subgrupoNombre === 'Manicura')
            },
            {
              id: 2,
              nombre: 'Pedicura',
              descripcion: 'Cuidado completo para pies.',
              ordenPublico: 20,
              servicios: this.LEGACY_SERVICES.filter(service => service.subgrupoNombre === 'Pedicura')
            },
            {
              id: 3,
              nombre: 'Extensiones',
              descripcion: 'Sets y mantenimientos.',
              ordenPublico: 30,
              servicios: this.LEGACY_SERVICES.filter(service => service.subgrupoNombre === 'Extensiones')
            }
          ]
        }
      ]
    };
  }
}
