import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ProductDiscountsAdminService, CatalogoDescuentosProducto, ReglaDescuentoProducto, ReglaDescuentoPayload } from './product-discounts-admin.service';

type FormDescuento=ReglaDescuentoPayload&{id:number|null};
@Component({selector:'app-admin-product-discounts',standalone:true,imports:[CommonModule,FormsModule],providers:[ProductDiscountsAdminService],templateUrl:'./admin-product-discounts.component.html',styleUrl:'./admin-product-discounts.component.css'})
export class AdminProductDiscountsComponent implements OnInit{
 private readonly service=inject(ProductDiscountsAdminService);
 readonly datos=signal<CatalogoDescuentosProducto|null>(null); readonly cargando=signal(true); readonly guardando=signal(false); readonly error=signal<string|null>(null); readonly mensaje=signal<string|null>(null); readonly form=signal<FormDescuento|null>(null);
 readonly vigentes=computed(()=>this.datos()?.reglas.filter(x=>x.estado==='VIGENTE').length??0);
 ngOnInit(){this.cargar();}
 nuevo(){this.form.set({id:null,nombre:'',descripcion:null,alcance:'GLOBAL',productoId:null,categoriaId:null,zonaPrecioId:null,sucursalId:null,tipoDescuento:'PORCENTAJE',valor:10,moneda:this.datos()?.monedaPredeterminada??'',cantidadMinima:1,fechaInicio:null,fechaFin:null,prioridad:0,combinable:false,activa:true});this.limpiar();}
 editar(item:ReglaDescuentoProducto){const {id,productoNombre,categoriaNombre,zonaPrecioNombre,sucursalNombre,estado,...payload}=item;this.form.set({id,...payload});this.limpiar();}
 cambiarAlcance(){this.form.update(f=>f?{...f,productoId:f.alcance==='PRODUCTO'?f.productoId:null,categoriaId:f.alcance==='CATEGORIA'?f.categoriaId:null}:null);}
 cambiarTipo(){this.form.update(f=>f?{...f,moneda:f.tipoDescuento==='PORCENTAJE'?null:(f.moneda||this.datos()?.monedaPredeterminada||'')}:null);}
 guardar(){const f=this.form();if(!f||!f.nombre.trim()||f.valor<=0||this.guardando())return;const {id,...payload}=f;this.guardando.set(true);this.limpiar();(id?this.service.actualizar(id,payload):this.service.crear(payload)).pipe(finalize(()=>this.guardando.set(false))).subscribe({next:()=>{this.form.set(null);this.cargar('Descuento guardado correctamente.');},error:e=>this.error.set(e?.error?.message||e?.error?.detail||'No fue posible guardar el descuento.')});}
 alcance(item:ReglaDescuentoProducto){if(item.alcance==='PRODUCTO')return item.productoNombre||'Producto';if(item.alcance==='CATEGORIA')return item.categoriaNombre||'Categoría';return 'Todo el catálogo';}
 valor(item:ReglaDescuentoProducto){return item.tipoDescuento==='PORCENTAJE'?`${item.valor}%`:new Intl.NumberFormat('es-MX',{style:'currency',currency:item.moneda||this.datos()?.monedaPredeterminada||'MXN'}).format(item.valor);}
 private cargar(mensaje?:string){this.cargando.set(true);this.service.catalogo().pipe(finalize(()=>this.cargando.set(false))).subscribe({next:d=>{this.datos.set(d);if(mensaje)this.mensaje.set(mensaje);},error:e=>this.error.set(e?.error?.message||'No fue posible cargar los descuentos.')});}
 private limpiar(){this.error.set(null);this.mensaje.set(null);}
}
