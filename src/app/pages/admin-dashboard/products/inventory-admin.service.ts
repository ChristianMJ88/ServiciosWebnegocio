import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export type EstadoInventario = 'DISPONIBLE' | 'BAJO' | 'AGOTADO';
export interface SucursalInventario { id: number; nombre: string; }
export interface ProductoInventario { id: number; sku: string; nombre: string; stockMinimo: number; }
export interface TipoMovimientoInventario { codigo: string; nombre: string; descripcion: string; }
export interface ExistenciaProducto {
  productoId: number; sku: string; productoNombre: string; sucursalId: number; sucursalNombre: string;
  existencia: number; reservado: number; disponible: number; stockMinimo: number; stockMaximo: number | null;
  estado: EstadoInventario; actualizadoEn: string | null;
}
export interface MovimientoInventario {
  id: number; productoId: number; productoNombre: string; sucursalId: number; sucursalNombre: string; tipo: string;
  cantidad: number; existenciaAnterior: number; existenciaNueva: number; referencia: string | null; notas: string | null; creadoEn: string;
}
export interface CatalogoInventario {
  sucursales: SucursalInventario[]; productos: ProductoInventario[]; existencias: ExistenciaProducto[];
  movimientos: MovimientoInventario[]; tiposMovimiento: TipoMovimientoInventario[];
}
export interface AjusteInventarioPayload { productoId: number; sucursalId: number; tipo: string; cantidad: number; referencia: string | null; notas: string | null; }
export interface TransferenciaInventarioPayload { productoId: number; sucursalOrigenId: number; sucursalDestinoId: number; cantidad: number; referencia: string | null; notas: string | null; }

@Injectable()
export class InventoryAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/admin/inventario-productos`;
  obtenerCatalogo() { return this.http.get<CatalogoInventario>(this.base); }
  ajustar(payload: AjusteInventarioPayload) { return this.http.post<ExistenciaProducto>(`${this.base}/ajustes`, payload); }
  transferir(payload: TransferenciaInventarioPayload) { return this.http.post<ExistenciaProducto[]>(`${this.base}/transferencias`, payload); }
}
