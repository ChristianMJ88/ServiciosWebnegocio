import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
import { environment } from '../../../environments/environment';

interface ClienteVenta { id:number;nombreCompleto:string;telefono:string; }
interface ProductoVenta { id:number;nombre:string;precio:number;moneda:string;disponible:number;controlaInventario:boolean; }
interface Cotizacion { subtotal:number;descuento:number;total:number;lineas:{productoId:number;total:number}[]; }
@Component({
 selector:'app-staff-sale-register',standalone:true,imports:[CommonModule,FormsModule,MoneyDisplayPipe],
 template:`<section class="sale-register">
 <header><h1>Registrar venta</h1><p>Selecciona el cliente y sus productos. Caja recibirá la cuenta para cobrarla.</p></header>
 @if(error()){<p class="notice error" role="alert">{{error()}}</p>}
 @if(mensaje()){<p class="notice" role="status">{{mensaje()}}</p>}
 <div class="sale-columns"><section>
 <h2>1. Cliente</h2>
 <form (ngSubmit)="buscarClientes()" class="search"><label class="sr-only" for="cliente-search">Nombre o teléfono del cliente</label><input id="cliente-search" name="buscarCliente" [(ngModel)]="busquedaCliente" placeholder="Nombre o teléfono"><button>Buscar</button></form>
 @if(cliente()){<div class="selected"><strong>{{cliente()!.nombreCompleto}}</strong><span>{{cliente()!.telefono}}</span><button type="button" (click)="cliente.set(null);cotizacion.set(null)">Cambiar cliente</button></div>}
 @else {<div class="client-results">@for(c of clientes();track c.id){<button class="client-result" type="button" (click)="seleccionarCliente(c)"><strong>{{c.nombreCompleto}}</strong><span>{{c.telefono}}</span></button>}</div>}
 <details><summary>Registrar cliente nuevo</summary><form #nuevo="ngForm" (ngSubmit)="crearCliente()" class="new-client">
 <label>Nombre completo<input name="nombre" [(ngModel)]="nombre" required maxlength="150"></label>
 <label>Teléfono<input name="telefono" [(ngModel)]="telefono" type="tel" required maxlength="30"></label>
 <label>Correo electrónico (opcional)<input name="correo" [(ngModel)]="correo" type="email"></label>
 <button [disabled]="nuevo.invalid || guardando()">Guardar cliente</button></form></details>
 <h2>2. Productos</h2>
 <label>Buscar producto<input [(ngModel)]="busquedaProducto" placeholder="Nombre del producto"></label>
 <div class="products">@for(p of productosFiltrados();track p.id){<button type="button" (click)="agregar(p)" [disabled]="guardando() || (p.controlaInventario && p.disponible<=0)"><strong>{{p.nombre}}</strong><span>{{p.precio | appMoney:p.moneda}}</span><small>{{p.controlaInventario ? 'Disponible: '+p.disponible : 'Agregar a la cuenta'}}</small></button>}</div>
 </section><aside><h2>3. Cuenta para Caja</h2>
 @for(l of carrito();track l.producto.id){<article><strong>{{l.producto.nombre}}</strong><div class="quantity"><button type="button" (click)="cantidad(l.producto.id,-1)" [disabled]="guardando()" aria-label="Reducir cantidad">−</button><span>{{l.cantidad}}</span><button type="button" (click)="cantidad(l.producto.id,1)" [disabled]="guardando()" aria-label="Aumentar cantidad">+</button></div></article>} @empty {<p>Agrega productos para preparar la cuenta.</p>}
 @if(cotizando()){<p role="status">Calculando precios y descuentos…</p>}
 @if(cotizacion()){<dl><dt>Subtotal</dt><dd>{{cotizacion()!.subtotal | appMoney:moneda()}}</dd><dt>Descuentos</dt><dd>{{cotizacion()!.descuento | appMoney:moneda()}}</dd><dt>Total por cobrar</dt><dd><strong>{{cotizacion()!.total | appMoney:moneda()}}</strong></dd></dl>}
 <button type="button" class="submit-sale" (click)="registrar()" [disabled]="guardando() || cotizando() || !cliente() || !cotizacion() || !carrito().length">{{guardando()?'Guardando…':'Enviar cuenta a Caja'}}</button>
 <p class="hint">Se guarda pendiente de cobro. El inventario y las comisiones se registran al pagar.</p>
 </aside></div></section>`,
 styles:[`:host{display:block}.sale-register{background:var(--bs-body-bg,#fff);padding:clamp(16px,3vw,28px);border:1px solid #e2e8f0;border-radius:24px}h1{font-size:1.6rem}h2{font-size:1.05rem;margin:24px 0 16px}p,small{color:#64748b}.sale-columns{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(280px,1fr);gap:28px}label,.new-client{display:grid;gap:8px}input,button{min-height:44px;border-radius:12px;border:1px solid #cbd5e1;padding:10px 12px}input{width:100%;min-width:0}button{background:#f8fafc;cursor:pointer}button:disabled{opacity:.45}.search{display:flex;gap:8px}.products{max-height:420px;overflow:auto;overscroll-behavior:contain;display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin-top:16px}.products button,.client-result,.selected{display:grid;gap:8px;text-align:left}.client-results{max-height:240px;overflow:auto;overscroll-behavior:contain}.client-result{width:100%;margin:8px 0}.selected{padding:16px;background:#f1f5f9;border-radius:16px}details{margin:20px 0}summary{cursor:pointer;padding:12px}.quantity{display:flex;gap:16px;align-items:center}article{display:flex;justify-content:space-between;align-items:center;gap:12px;border-bottom:1px solid #e2e8f0;padding:16px 0}dl{display:grid;grid-template-columns:1fr auto;gap:12px;margin:24px 0}dd{margin:0}.submit-sale{width:100%;background:var(--bs-primary,#2563eb);color:white}.hint{font-size:.85rem}.notice{padding:14px;border-radius:12px;background:#ecfdf5}.notice.error{background:#fff1f2}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)}@media(max-width:800px){.sale-columns{grid-template-columns:1fr}.products{grid-template-columns:repeat(auto-fit,minmax(135px,1fr))}aside{border-top:1px solid #e2e8f0}.submit-sale{box-shadow:0 8px 24px #0002}}`]
})
export class StaffSaleRegisterComponent implements OnInit {
 private readonly http=inject(HttpClient);private readonly destroyRef=inject(DestroyRef);
 private readonly base=environment.apiBaseUrl+'/staff/ventas';private revision=0;
 readonly clientes=signal<ClienteVenta[]>([]);readonly cliente=signal<ClienteVenta|null>(null);
 readonly productos=signal<ProductoVenta[]>([]);readonly carrito=signal<{producto:ProductoVenta;cantidad:number}[]>([]);
 readonly cotizacion=signal<Cotizacion|null>(null);readonly cotizando=signal(false);readonly guardando=signal(false);
 readonly error=signal('');readonly mensaje=signal('');
 busquedaCliente='';busquedaProducto='';nombre='';telefono='';correo='';
 ngOnInit(){this.http.get<{productos:ProductoVenta[]}>(this.base+'/productos').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:r=>this.productos.set(r.productos),error:e=>this.fallo(e)});}
 productosFiltrados(){return this.productos().filter(p=>p.nombre.toLocaleLowerCase().includes(this.busquedaProducto.toLocaleLowerCase()));}
 moneda(){return this.carrito()[0]?.producto.moneda;}
 buscarClientes(){this.http.get<{content:ClienteVenta[]}>(this.base+'/clientes',{params:new HttpParams().set('busqueda',this.busquedaCliente)}).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:r=>this.clientes.set(r.content),error:e=>this.fallo(e)});}
 seleccionarCliente(c:ClienteVenta){this.cliente.set(c);this.cotizar();}
 crearCliente(){this.guardando.set(true);this.http.post<ClienteVenta>(this.base+'/clientes',{nombreCompleto:this.nombre,telefono:this.telefono,correo:this.correo || null}).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:c=>{this.guardando.set(false);this.seleccionarCliente(c);this.nombre='';this.telefono='';this.correo='';},error:e=>{this.guardando.set(false);this.fallo(e);}});}
 agregar(p:ProductoVenta){const existente=this.carrito().find(l=>l.producto.id===p.id);if(existente){this.cantidad(p.id,1);return;}this.carrito.update(ls=>[...ls,{producto:p,cantidad:1}]);this.cotizar();}
 cantidad(id:number,delta:number){this.carrito.update(ls=>ls.flatMap(l=>l.producto.id!==id?[l]:l.cantidad+delta>0?[{...l,cantidad:l.cantidad+delta}]:[]));this.cotizar();}
 payload(){return {clienteId:this.cliente()?.id,productos:this.carrito().map(l=>({productoId:l.producto.id,cantidad:l.cantidad}))};}
 cotizar(){const rev=++this.revision;this.cotizacion.set(null);this.cotizando.set(false);if(!this.cliente()||!this.carrito().length)return;this.cotizando.set(true);this.http.post<Cotizacion>(this.base+'/cotizacion',this.payload()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:r=>{if(rev===this.revision){this.cotizacion.set(r);this.cotizando.set(false);}},error:e=>{if(rev===this.revision){this.cotizando.set(false);this.fallo(e);}}});}
 registrar(){if(this.guardando()||!this.cotizacion()||!this.cliente())return;this.guardando.set(true);this.error.set('');this.http.post<{folio:string}>(this.base,this.payload()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next:r=>{this.guardando.set(false);this.mensaje.set('Cuenta '+r.folio+' enviada a Caja, pendiente de pago.');this.carrito.set([]);this.cotizacion.set(null);this.cliente.set(null);this.clientes.set([]);},error:e=>{this.guardando.set(false);this.fallo(e);}});}
 private fallo(e:any){this.error.set(e.error?.detail||e.error?.message||'No fue posible completar la operación.');}
}
