import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface OpcionDescuento { id:number; nombre:string; }
export interface ReglaDescuentoProducto {
  id:number; nombre:string; descripcion:string|null; alcance:'GLOBAL'|'PRODUCTO'|'CATEGORIA'; productoId:number|null; productoNombre:string|null;
  categoriaId:number|null; categoriaNombre:string|null; zonaPrecioId:number|null; zonaPrecioNombre:string|null; sucursalId:number|null; sucursalNombre:string|null;
  tipoDescuento:'PORCENTAJE'|'MONTO'|'PRECIO_FIJO'; valor:number; moneda:string|null; cantidadMinima:number; fechaInicio:string|null; fechaFin:string|null;
  prioridad:number; combinable:boolean; activa:boolean; estado:'VIGENTE'|'PROGRAMADA'|'EXPIRADA'|'INACTIVA';
}
export interface CatalogoDescuentosProducto { monedaPredeterminada:string; productos:OpcionDescuento[]; categorias:OpcionDescuento[]; zonasPrecio:OpcionDescuento[]; sucursales:OpcionDescuento[]; reglas:ReglaDescuentoProducto[]; }
export type ReglaDescuentoPayload = Omit<ReglaDescuentoProducto,'id'|'productoNombre'|'categoriaNombre'|'zonaPrecioNombre'|'sucursalNombre'|'estado'>;

@Injectable()
export class ProductDiscountsAdminService {
  private readonly http=inject(HttpClient);
  private readonly base=`${environment.apiBaseUrl}/admin/productos/descuentos`;
  catalogo(){return this.http.get<CatalogoDescuentosProducto>(this.base);}
  crear(payload:ReglaDescuentoPayload){return this.http.post<ReglaDescuentoProducto>(this.base,payload);}
  actualizar(id:number,payload:ReglaDescuentoPayload){return this.http.patch<ReglaDescuentoProducto>(`${this.base}/${id}`,payload);}
}
