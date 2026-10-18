import {Component,Input,Output,EventEmitter,OnChanges,SimpleChanges} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {OpcionCaja,TerminalPointCaja,OrdenPointCaja,VentaCaja,PagoVentaPayload,ResolverPointPayload} from '../../../core/caja/caja.service';
import {MoneyDisplayPipe} from '../../../shared/pipes/money-display.pipe';
import {VentaComprobanteComponent} from '../../../shared/components/venta-comprobante.component';
export interface SolicitudCobro {pagos:PagoVentaPayload[];terminalId:number|null;observaciones:string;usaPoint:boolean;}
interface LineaCobro {metodoPago:string;monto:number|null;recibido:number|null;referencia:string;}
@Component({selector:'app-caja-cobro',standalone:true,imports:[FormsModule,MoneyDisplayPipe,VentaComprobanteComponent],
 templateUrl:'./caja-cobro.component.html',styleUrl:'./caja-cobro.component.css'})
export class CajaCobroComponent implements OnChanges {
 @Input() total=0;
 @Input() moneda='';
 @Input() identidad:string|number|null=null;
 @Input() metodos:OpcionCaja[]=[];
 @Input() terminales:TerminalPointCaja[]=[];
 @Input() permiteAbono=false;
 @Input() bloqueo='';
 @Input() procesando=false;
 @Input() orden:OrdenPointCaja|null=null;
 @Input() comprobante:VentaCaja|null=null;
 @Input() error='';
 @Input() simulando=false;
 @Input() accionPointEnCurso=false;
 @Output() cobrar=new EventEmitter<SolicitudCobro>();
 @Output() reintentarPoint=new EventEmitter<void>();
 @Output() resolverPoint=new EventEmitter<ResolverPointPayload>();
 resolucionCobrado=false;resolucionProveedor='';resolucionMotivo='';verificado=false;
 resolver(){if(!this.orden?.puedeResolver || !this.verificado || this.resolucionMotivo.trim().length<10 || this.resolucionCobrado&&!this.resolucionProveedor.trim())return;this.resolverPoint.emit({cobrado:this.resolucionCobrado,proveedorOrdenId:this.resolucionProveedor.trim()||null,motivo:this.resolucionMotivo.trim()});}
 @Output() cancelarPoint=new EventEmitter<void>();
 @Output() simularPoint=new EventEmitter<void>();
 @Output() nuevo=new EventEmitter<void>();
 lineas:LineaCobro[]=[];terminalId:number|null=null;notas='';
 ngOnChanges(c:SimpleChanges) {
  if(c['orden'] && c['orden'].previousValue?.id!==c['orden'].currentValue?.id){this.resolucionCobrado=false;this.resolucionProveedor='';this.resolucionMotivo='';this.verificado=false;}
  if(c['identidad'] || !this.lineas.length) this.reiniciar();
  if(c['total'] && this.lineas.length===1) {this.lineas[0].monto=this.total;this.lineas[0].recibido=this.total;}
  if(c['terminales'] && !this.terminales.some(t=>t.id===this.terminalId)) this.terminalId=this.terminales.length===1?this.terminales[0].id:null;
  if(c['metodos'] && this.lineas.length===1 && !this.opcion(this.lineas[0])) this.reiniciar();
 }
 get disponibles():OpcionCaja[] {
  const point=this.terminales.find(t=>t.activa)?.metodoPago;
  return [...this.metodos.filter(m=>!m.esPoint || this.terminales.some(t=>t.activa)),...(point && !this.metodos.some(m=>m.codigo===point.codigo)?[point]:[])];
 }
 opcion(l:LineaCobro) {return this.disponibles.find(m=>m.codigo===l.metodoPago);}
 get dividida(){return this.lineas.length>1;}
 private redondear(n:number){return Math.round((n+Number.EPSILON)*100)/100;}
 importe(l:LineaCobro){return this.opcion(l)?.esPoint?this.redondear(this.total-this.lineas.filter(o=>o!==l).reduce((t,o)=>t+Number(o.monto||0),0)):Number(l.monto||0);}
 cambio(l:LineaCobro){return this.opcion(l)?.esEfectivo?Math.max(0,this.redondear(Number(l.recibido||0)-this.importe(l))):0;}
 get restante(){return this.redondear(this.total-this.lineas.reduce((t,l)=>t+this.importe(l),0));}
 get esperando(){return !!this.orden && !['APROBADO','CANCELADO','RECHAZADO','EXPIRADO','REEMBOLSADO'].includes(this.orden.estado);}
 get motivo():string {
  if(this.bloqueo)return this.bloqueo;
  if(this.procesando)return 'Procesando el cobro…';
  if(this.total<=0)return 'Selecciona conceptos para cobrar.';
  for(const l of this.lineas) {
   const m=this.opcion(l);if(!m)return 'Selecciona un método de pago.';
   if(this.importe(l)<=0)return 'Cada parte debe ser mayor que cero.';
   if(m.esPoint && !this.terminalId)return 'Selecciona la terminal.';
   if(m.requiereReferencia && !l.referencia.trim())return 'Captura la referencia del pago.';
   if(m.esEfectivo && Number(l.recibido||0)<this.importe(l))return 'El efectivo recibido no alcanza.';
  }
  if(this.dividida && this.restante!==0)return 'Reparte el total entre los métodos seleccionados.';
  if(!this.dividida && (!this.permiteAbono || this.importe(this.lineas[0])>this.total) && this.restante!==0)return 'El importe debe coincidir con el saldo.';
  return '';
 }
 get cta(){return this.lineas.map((l,i)=>`${i?'y ':''}${this.opcion(l)?.esPoint?'enviar':'cobrar'} ${this.dinero(this.importe(l))} ${this.opcion(l)?.esPoint?'a':'en'} ${this.opcion(l)?.etiqueta??''}`).join(' ').replace(/^./,c=>c.toUpperCase());}
 dinero(n:number){try{return new Intl.NumberFormat('es-MX',{style:'currency',currency:this.moneda,currencyDisplay:'narrowSymbol',minimumFractionDigits:2}).format(n);}catch{return String(n);}}
 atajos(l:LineaCobro){return [...new Set((this.opcion(l)?.denominaciones?.[this.moneda]??[]).filter(n=>n>0).map(n=>Math.ceil(this.importe(l)/n)*n))].filter(n=>n>this.importe(l)).sort((a,b)=>a-b);}
 elegir(l:LineaCobro,codigo:string){
  const opcion=this.disponibles.find(m=>m.codigo===codigo);if(!opcion)return;
  if(opcion.esPoint && this.lineas.some(o=>o!==l && this.opcion(o)?.esPoint))return;
  l.metodoPago=codigo;l.referencia='';l.recibido=this.importe(l);
  if(opcion.esPoint && this.dividida)this.lineas=[...this.lineas.filter(o=>o!==l),l];
 }
 cambiarMonto(l:LineaCobro,n:number|null){l.monto=n;if(this.dividida && l===this.lineas[0] && !this.opcion(this.lineas[1])?.esPoint) {this.lineas[1].monto=this.redondear(this.total-Number(n||0));this.lineas[1].recibido=this.lineas[1].monto;}}
 dividir(){
  if(this.dividida || this.esperando)return;
  const primera=this.lineas[0];const otra=this.disponibles.find(m=>m.codigo!==primera.metodoPago);
  if(!otra)return;
  if(this.opcion(primera)?.esPoint) this.lineas=[{metodoPago:otra.codigo,monto:null,recibido:null,referencia:''},primera];
  else {primera.monto=null;primera.recibido=null;this.lineas.push({metodoPago:otra.codigo,monto:this.total,recibido:this.total,referencia:''});}
 }
 quitar(indice:number){this.lineas.splice(indice,1);this.lineas[0].monto=this.total;this.lineas[0].recibido=this.total;}
 enviar(){
  if(this.motivo || this.esperando)return;
  this.cobrar.emit({pagos:this.lineas.map(l=>({metodoPago:l.metodoPago,monto:this.opcion(l)?.esPoint?null:this.importe(l),montoRecibido:this.opcion(l)?.esEfectivo?l.recibido:null,referencia:l.referencia.trim()||null})),terminalId:this.terminalId,observaciones:this.notas,usaPoint:this.lineas.some(l=>this.opcion(l)?.esPoint)});
 }
 reiniciar(){const metodo=this.disponibles.find(m=>m.esEfectivo)??this.disponibles[0];this.lineas=[{metodoPago:metodo?.codigo??'',monto:this.total,recibido:this.total,referencia:''}];this.notas='';}
 nueva(){this.reiniciar();this.nuevo.emit();}
}
