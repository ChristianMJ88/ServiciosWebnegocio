import { environment } from '../../../environments/environment';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';

interface VentaStaff { id:number; folio:string; nombre:string; concepto:string; estado:string; moneda:string; cantidad:number; total:number; fecha:string; }
interface PaginaVentas { content:VentaStaff[]; totalElements:number; totalPages:number; }
@Component({
  selector:'app-staff-sales-section', standalone:true, imports:[CommonModule,FormsModule,MoneyDisplayPipe],
  template:`<section class="sales-panel">
    <header><div><h2>{{ reporte ? 'Reporte de ventas' : 'Mis ventas' }}</h2><p>Servicios y productos atribuidos a ti. El cobro y su estado se registran en Caja.</p></div><button type="button" (click)="cargar()" [disabled]="cargando()">Actualizar</button></header>
    <form (ngSubmit)="filtrar()" class="sales-filters">
      <label>Buscar<input name="busqueda" [(ngModel)]="busqueda" placeholder="Producto, servicio o folio"></label>
      <label>Concepto<select name="concepto" [(ngModel)]="concepto"><option value="">Todos</option><option value="SERVICIO">Servicios</option><option value="PRODUCTO">Productos</option></select></label>
      <label>Desde<input type="date" name="desde" [(ngModel)]="desde"></label>
      <label>Hasta<input type="date" name="hasta" [(ngModel)]="hasta"></label>
      <button type="submit" [disabled]="cargando()">Aplicar filtros</button>
    </form>
    @if(error()) {<p role="alert">{{ error() }}</p>}
    @if(cargando()) {<p role="status">Consultando ventas…</p>}
    @else {
      <p>{{ total() }} registros encontrados</p>
      <div class="sales-list">@for(venta of filas(); track venta.id) {
        <article><div><small>{{ venta.folio }} · {{ venta.fecha | date:'dd/MM/yyyy HH:mm' }}</small><h3>{{ venta.nombre }}</h3><span>{{ venta.concepto }} · Cantidad {{ venta.cantidad }}</span></div><div class="sales-value"><strong>{{ venta.total | appMoney:venta.moneda }}</strong><span>{{ venta.estado }}</span></div></article>
      } @empty {<p>No hay ventas atribuidas a ti en este periodo.</p>}</div>
      <nav aria-label="Paginación de ventas"><button (click)="mover(-1)" [disabled]="pagina===0">Anterior</button><span>Página {{ pagina+1 }} de {{ paginas() || 1 }}</span><button (click)="mover(1)" [disabled]="pagina+1>=paginas()">Siguiente</button></nav>
    }
  </section>`,
  styles:[`:host{display:block}.sales-panel{padding:clamp(16px,3vw,28px);background:var(--bs-body-bg,#fff);border:1px solid #e2e8f0;border-radius:24px}header,article,nav{display:flex;justify-content:space-between;gap:16px;align-items:center}h2,h3,p{margin:0 0 8px}h3{font-size:1rem}p,small,article span{color:#64748b}button,input,select{min-height:44px;border:1px solid #cbd5e1;border-radius:12px;padding:10px 14px}button{cursor:pointer;background:#f1f5f9}button:disabled{opacity:.45}.sales-filters{display:grid;grid-template-columns:2fr 1fr 1fr 1fr auto;gap:12px;align-items:end;margin:24px 0}label{display:grid;gap:6px;font-size:.85rem}input{width:100%;min-width:0}.sales-list{display:grid;gap:12px}article{padding:16px;border:1px solid #e2e8f0;border-radius:16px}.sales-value{display:grid;gap:8px;text-align:right;flex-shrink:0}nav{margin-top:24px;flex-wrap:wrap}@media(max-width:900px){.sales-filters{grid-template-columns:1fr 1fr}.sales-filters label:first-child{grid-column:1/-1}}@media(max-width:480px){header{align-items:flex-start}header p{font-size:.85rem}article{align-items:flex-start}.sales-value strong{font-size:1rem}nav{font-size:.8rem}}`]
})
export class StaffSalesSectionComponent implements OnInit {
  @Input() reporte=false;
  private readonly http=inject(HttpClient);
  private readonly destroyRef=inject(DestroyRef);
  readonly filas=signal<VentaStaff[]>([]); readonly total=signal(0); readonly paginas=signal(0);
  readonly cargando=signal(false); readonly error=signal('');
  busqueda=''; concepto=''; desde=''; hasta=''; pagina=0;
  ngOnInit(){this.cargar();}
  filtrar(){this.pagina=0;this.cargar();}
  mover(delta:number){this.pagina+=delta;this.cargar();}
  cargar(){
    if(this.cargando())return;
    this.cargando.set(true);this.error.set('');
    let params=new HttpParams().set('pagina',this.pagina).set('busqueda',this.busqueda).set('concepto',this.concepto);
    if(this.desde)params=params.set('desde',this.desde);
    if(this.hasta)params=params.set('hasta',this.hasta);
    this.http.get<PaginaVentas>(environment.apiBaseUrl+'/staff/ventas'+(this.reporte?'/reporte':''),{params}).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:p=>{this.filas.set(p.content);this.total.set(p.totalElements);this.paginas.set(p.totalPages);this.cargando.set(false);},
      error:e=>{this.error.set(e.error?.message || 'No se pudieron consultar las ventas.');this.cargando.set(false);}
    });
  }
}
