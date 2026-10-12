import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TenantStoreConfig } from '../core/tenant/tenant.types';

export interface StoreZone { id: number; nombre: string; clave: string; }
export interface StoreCategory { id: number; categoriaPadreId: number | null; nombre: string; slug: string; descripcion: string | null; }
export interface StoreProduct {
  id: number;
  categoriaId: number | null;
  categoriaNombre: string | null;
  nombre: string;
  slug: string;
  descripcion: string | null;
  imagenUrl: string | null;
  precio: number;
  precioAnterior: number | null;
  moneda: string;
  descuento: string | null;
  controlaInventario: boolean;
  disponible: boolean;
  existencia: number | null;
  nuevo?: boolean;
}
export interface PublicProductCatalog {
  empresaId: number;
  slug: string;
  nombreComercial: string;
  tienda: TenantStoreConfig;
  zonaSeleccionada: StoreZone | null;
  zonas: StoreZone[];
  categorias: StoreCategory[];
  productos: StoreProduct[];
  sucursales: {id:number;nombre:string}[];
  sucursalSeleccionadaId: number | null;
}

@Injectable({ providedIn: 'root' })
export class ProductStoreService {
  private readonly http = inject(HttpClient);

  catalog(slug: string, zone?: string | null, productSlug?: string | null, branchId?: string | null): Observable<PublicProductCatalog> {
    let params = new HttpParams();
    if (branchId) params=params.set('sucursalId',branchId);
    if (zone) params = params.set('zona', zone);
    const path = productSlug
      ? `/publico/productos/sitio/${encodeURIComponent(slug)}/${encodeURIComponent(productSlug)}`
      : `/publico/productos/sitio/${encodeURIComponent(slug)}`;
    return this.http.get<PublicProductCatalog>(`${environment.apiBaseUrl}${path}`, { params });
  }
}
