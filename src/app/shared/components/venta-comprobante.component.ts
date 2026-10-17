import { Component, Input } from '@angular/core';
import { VentaCaja } from '../../core/caja/caja.service';
import { MoneyDisplayPipe } from '../pipes/money-display.pipe';
@Component({selector:'app-venta-comprobante',standalone:true,imports:[MoneyDisplayPipe],template:`
@if(venta) {<section class="receipt" aria-label="Comprobante de venta"><h3>Comprobante {{venta.folio}}</h3>
@for(d of venta.detalles; track d.id) {<div><span>{{d.nombre}} · {{d.cantidad}}</span><strong>{{d.total | appMoney:venta.moneda:2:2}}</strong></div>}
<div><span>Total cobrado</span><strong>{{venta.total | appMoney:venta.moneda:2:2}}</strong></div><p>{{venta.metodoPago}} {{venta.referencia}}</p>
<button type="button" class="btn btn-outline-primary" (click)="imprimir()">Imprimir comprobante</button></section>}`,
styles:[`.receipt{padding:1rem;border:1px solid var(--bs-border-color,#ddd);border-radius:1rem;margin:1rem 0}.receipt div{display:flex;justify-content:space-between;gap:1rem;padding:.5rem 0}.receipt h3{font-size:1rem}@media print{button{display:none}}`]})
export class VentaComprobanteComponent { @Input() venta: VentaCaja | null=null; imprimir(){window.print();} }
