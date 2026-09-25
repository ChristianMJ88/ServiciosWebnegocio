import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ArchivoMediaAdmin } from '../../../core/admin/admin.service';

export interface CategoriaProductoAdmin { id: number; categoriaPadreId: number | null; nombre: string; slug: string; descripcion: string | null; orden: number; activa: boolean; }
export interface ProveedorProductoAdmin { id: number; nombre: string; clave: string | null; contacto: string | null; correo: string | null; telefono: string | null; sitioWeb: string | null; notas: string | null; activo: boolean; }
export interface ZonaPrecioProductoAdmin { id: number; nombre: string; clave: string; descripcion: string | null; prioridad: number; activa: boolean; }
export interface PrecioZonaProductoAdmin { id?: number; zonaPrecioId: number; zonaNombre?: string | null; precio: number; moneda: string; activo: boolean; }
export interface ProductoAdmin {
  id: number; categoriaId: number | null; categoriaNombre: string | null; proveedorId: number | null; proveedorNombre: string | null;
  mediaId: number | null; sku: string; codigoBarras: string | null; nombre: string; slug: string; descripcion: string | null;
  imagenUrl: string | null; precioBase: number; costo: number | null; moneda: string; tasaImpuesto: number; controlaInventario: boolean;
  stockMinimo: number; visiblePublico: boolean; activo: boolean; preciosZona: PrecioZonaProductoAdmin[];
}
export interface CatalogoProductosAdmin { monedaPredeterminada: string; categorias: CategoriaProductoAdmin[]; proveedores: ProveedorProductoAdmin[]; zonasPrecio: ZonaPrecioProductoAdmin[]; productos: ProductoAdmin[]; }
export type CategoriaProductoPayload = Omit<CategoriaProductoAdmin, 'id'>;
export type ProveedorProductoPayload = Omit<ProveedorProductoAdmin, 'id'>;
export type ZonaPrecioProductoPayload = Omit<ZonaPrecioProductoAdmin, 'id'>;
export type ProductoPayload = Omit<ProductoAdmin, 'id' | 'categoriaNombre' | 'proveedorNombre'>;

@Injectable()
export class ProductsAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/admin/productos`;

  obtenerCatalogo(): Observable<CatalogoProductosAdmin> { return this.http.get<CatalogoProductosAdmin>(`${this.base}/catalogo`); }
  crearCategoria(payload: CategoriaProductoPayload) { return this.http.post<CategoriaProductoAdmin>(`${this.base}/categorias`, payload); }
  actualizarCategoria(id: number, payload: CategoriaProductoPayload) { return this.http.patch<CategoriaProductoAdmin>(`${this.base}/categorias/${id}`, payload); }
  crearProveedor(payload: ProveedorProductoPayload) { return this.http.post<ProveedorProductoAdmin>(`${this.base}/proveedores`, payload); }
  actualizarProveedor(id: number, payload: ProveedorProductoPayload) { return this.http.patch<ProveedorProductoAdmin>(`${this.base}/proveedores/${id}`, payload); }
  crearZona(payload: ZonaPrecioProductoPayload) { return this.http.post<ZonaPrecioProductoAdmin>(`${this.base}/zonas-precio`, payload); }
  actualizarZona(id: number, payload: ZonaPrecioProductoPayload) { return this.http.patch<ZonaPrecioProductoAdmin>(`${this.base}/zonas-precio/${id}`, payload); }
  crearProducto(payload: ProductoPayload) { return this.http.post<ProductoAdmin>(this.base, payload); }
  actualizarProducto(id: number, payload: ProductoPayload) { return this.http.patch<ProductoAdmin>(`${this.base}/${id}`, payload); }
  subirImagen(archivo: File): Observable<ArchivoMediaAdmin> {
    const formulario = new FormData();
    formulario.append('archivo', archivo, archivo.name);
    return this.http.post<ArchivoMediaAdmin>(`${this.base}/media/imagenes`, formulario);
  }
}
