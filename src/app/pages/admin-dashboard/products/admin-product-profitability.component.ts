import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AdminProductRealizedProfitComponent } from './admin-product-realized-profit.component';
interface Fila { productoId:number; sku:string; nombre:string; categoria:string|null; moneda:string; precio:number; costo:number|null; diferencia:number|null; margenPorcentaje:number|null; }
interface Reporte { filas:Fila[]; pagina:number; totalPaginas:number; totalElementos:number; alcance:string; }
@Component({selector:'app-admin-product-profitability', standalone:true, imports:[CommonModule, FormsModule, AdminProductRealizedProfitComponent], template:`
<nav aria-label="Tipo de reporte" class="report-modes"><button type="button" [attr.aria-pressed]="!realizada()" (click)="realizada.set(false)">Estimación del catálogo</button><button type="button" [attr.aria-pressed]="realizada()" (click)="realizada.set(true)">Utilidad realizada</button></nav>
@if(realizada()){<app-admin-product-realized-profit />}@else{<section class="profit-report" aria-label="Rentabilidad de productos">
<header><div><small>ANÁLISIS DEL CATÁLOGO</small><h2>Rentabilidad estimada</h2></div><button (click)="cargar(0)" [disabled]="cargando()">Actualizar</button></header>
<form (ngSubmit)="cargar(0)"><label>Buscar producto<input name="busquedaRentabilidad" [(ngModel)]="busqueda" placeholder="Nombre, SKU, categoría o proveedor"></label><button [disabled]="cargando()">Buscar</button></form>
@if(error()){<p role="alert">{{error()}}</p>}
@if(cargando()){<p role="status">Consultando reporte…</p>}
@if(reporte(); as r){<p class="notice">{{r.alcance}}</p><p>{{r.totalElementos}} productos · Importes unitarios</p>
<div class="profit-grid">@for(f of r.filas; track f.productoId){<article><small>{{f.sku}} · {{f.categoria || 'Sin categoría'}}</small><h3>{{f.nombre}}</h3><dl><div><dt>Precio base</dt><dd>{{f.precio | currency:f.moneda:'symbol':'1.2-2':'es-MX'}}</dd></div><div><dt>Costo actual</dt><dd>{{f.costo === null ? 'Sin costo registrado' : (f.costo | currency:f.moneda:'symbol':'1.2-2':'es-MX')}}</dd></div><div><dt>Diferencia estimada</dt><dd>{{f.diferencia === null ? 'No calculable' : (f.diferencia | currency:f.moneda:'symbol':'1.2-2':'es-MX')}}</dd></div><div><dt>Margen estimado</dt><dd>{{f.margenPorcentaje === null ? 'No calculable' : (f.margenPorcentaje | number:'1.2-2') + '%'}}</dd></div></dl></article>}@empty{<p>No hay coincidencias.</p>}</div>
@if(r.totalPaginas > 0){<nav aria-label="Paginación del reporte"><button (click)="cargar(r.pagina-1)" [disabled]="cargando() || r.pagina===0">Anterior</button><span>Página {{r.pagina+1}} de {{r.totalPaginas}}</span><button (click)="cargar(r.pagina+1)" [disabled]="cargando() || r.pagina+1>=r.totalPaginas">Siguiente</button></nav>}}
</section>}`, styles:[`
.report-modes{margin-bottom:1rem;justify-content:flex-start}.report-modes [aria-pressed=true]{background:#244ac1;color:white}.profit-report{display:grid;gap:1rem;padding:1.2rem;background:#fff;border-radius:20px}header,form,nav{display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap}h2,h3{margin:.3rem 0}label{display:grid;gap:.4rem;flex:1;min-width:0}input,button{min-height:44px;border:1px solid #dbe3ef;border-radius:12px;padding:.6rem .9rem}button{background:#eff4ff;color:#244ac1}button:disabled{opacity:.5}.notice{background:#f5f7fc;border-left:3px solid #5270df;padding:1rem;line-height:1.6}.profit-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,280px),1fr));gap:1rem}article{padding:1rem;border:1px solid #e1e7f2;border-radius:16px}dl{display:grid;gap:.8rem;margin-bottom:0}dl div{display:flex;justify-content:space-between;gap:1rem}dd{margin:0;font-weight:700;text-align:right}small,dt{color:#64748b}@media(max-width:600px){form{align-items:stretch}form label{flex-basis:100%}nav{gap:.5rem;font-size:.85rem}}
`]})
export class AdminProductProfitabilityComponent implements OnInit {
 readonly realizada=signal(false);
 private readonly http=inject(HttpClient);
 readonly reporte=signal<Reporte|null>(null); readonly cargando=signal(false); readonly error=signal<string|null>(null); busqueda='';
 ngOnInit(){this.cargar(0);}
 cargar(pagina:number){if(this.cargando())return;this.cargando.set(true);this.error.set(null);this.http.get<Reporte>('/api/v1/admin/productos/rentabilidad',{params:new HttpParams().set('pagina',pagina).set('busqueda',this.busqueda)}).pipe(finalize(()=>this.cargando.set(false))).subscribe({next:r=>this.reporte.set(r),error:()=>this.error.set('No fue posible consultar la rentabilidad.')});}
}
