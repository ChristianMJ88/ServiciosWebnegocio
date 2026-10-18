import {ResolverPointPayload} from '../../../core/caja/caja.service';
import {CajaCobroComponent,SolicitudCobro} from '../cobro/caja-cobro.component';
import { VentaComprobanteComponent } from '../../../shared/components/venta-comprobante.component';
import { ETIQUETAS_COBRO } from '../../../core/caja/cobro-cita.models';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Subscription,finalize, switchMap, takeWhile, timer } from 'rxjs';
import { CajaService, CitaPorCobrar, OpcionCaja, OrdenPointCaja, PagoCita, TerminalPointCaja, VentaCaja } from '../../../core/caja/caja.service';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
import { FormularioPagoCaja } from '../forms/caja.forms';

@Component({
  selector: 'app-caja-payments-section',
  standalone: true,
  imports: [
    CajaCobroComponent, VentaComprobanteComponent, CommonModule,
    DatePipe,
    FormsModule,
    MoneyDisplayPipe,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './caja-payments-section.component.html',
  styleUrls: ['./caja-payments-section.component.css']
})
export class CajaPaymentsSectionComponent implements OnChanges {
  readonly etiquetasCobro = ETIQUETAS_COBRO;
  private readonly caja = inject(CajaService);
  private readonly destroyRef = inject(DestroyRef);
  @Input() mostrarCobroInicial = false;
  readonly mostrandoCobro = signal(false);
  seleccionar(citaId: number): void { this.mostrandoCobro.set(true); this.selectAppointment.emit(citaId); }
  busqueda = '';
  adicionales: number[] = [];
  readonly procesandoVenta = signal(false);
  readonly ventaCobrada = signal<VentaCaja | null>(null);
  get otrasCitas() {const c=this.citaSeleccionada; return c ? this.citas.filter(o=>o.citaId!==c.citaId && o.clienteId===c.clienteId && o.sucursalId===c.sucursalId && o.moneda===c.moneda) : [];}
  get seleccionadas() {return this.citaSeleccionada ? [this.citaSeleccionada,...this.otrasCitas.filter(c=>this.adicionales.includes(c.citaId))] : [];}
  get cobroMultiple() {return this.seleccionadas.length > 1;}
  get totalSeleccionado() {return this.seleccionadas.reduce((t,c)=>t + Number(c.pendiente),0);}
  alternarCita(id:number) {
    if(this.procesandoVenta() || this.guardando || this.procesandoPoint()) return;
    this.adicionales=this.adicionales.includes(id)?this.adicionales.filter(i=>i!==id):[...this.adicionales,id];
    if(this.cobroMultiple) this.formularioPago.monto=this.totalSeleccionado;
    else this.formularioPago.monto=this.citaSeleccionada?.pendiente ?? 0;
  }
  get bloqueoPanel(){return !this.cajaAbierta?this.mensajeBloqueo||'Abre la caja para cobrar.':this.seleccionadas.some(c=>c.cobroPointEnCurso)?'Cobro Point en curso. Espera a que termine o cancélalo en Actividad del punto de venta.':'';}
  cobrarDesdePanel(s:SolicitudCobro){
    if(this.bloqueoPanel)return;
    this.terminalPointId=s.terminalId;
    if(s.usaPoint){this.cobrarConPoint(s);return;}
    const p=s.pagos[0];
    if(!this.cobroMultiple && s.pagos.length===1 && Number(p.monto)<this.totalSeleccionado){
      Object.assign(this.formularioPago,{monto:p.monto,metodoPago:p.metodoPago,montoRecibido:p.montoRecibido??0,referencia:p.referencia??'',observaciones:s.observaciones});this.savePayment.emit();return;
    }
    this.registrarCobro(s);
  }
  registrarCobro(s?:SolicitudCobro) {
    if(this.guardando || this.procesandoVenta() || this.procesandoPoint() || !this.cajaAbierta) return;
    if(!s && !this.cobroMultiple) {this.savePayment.emit(); return;}
    if(!this.sucursalId || (!s && !this.formularioPago.metodoPago)) return;
    this.procesandoVenta.set(true); this.errorPoint.set('');
    this.caja.registrarVenta({sucursalId:this.sucursalId,citaId:null,citaIds:this.seleccionadas.map(c=>c.citaId),clienteId:null,productos:[],...(s?{pagos:s.pagos}:{metodoPago:this.formularioPago.metodoPago}),referencia:s?null:this.formularioPago.referencia || null,observaciones:s?.observaciones || this.formularioPago.observaciones || null})
      .pipe(finalize(()=>this.procesandoVenta.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({
        next:v=>{this.ventaCobrada.set(v);this.adicionales=[];this.pointPaymentCompleted.emit();},
        error:e=>this.errorPoint.set(e.error?.mensaje || e.error?.message || e.error?.detail || 'No se pudo cobrar la cuenta completa.')
      });
  }
  get gruposPendientes() {
    const texto = this.busqueda.trim().toLocaleLowerCase();
    const visibles = this.citas.filter(c => !texto || `${c.folio ?? ''} ${c.clienteNombre} ${c.clienteTelefono ?? ''}`.toLocaleLowerCase().includes(texto));
    return [
      {codigo:'HOY', titulo:'Hoy', citas:visibles.filter(c => c.periodoCobro === 'HOY')},
      {codigo:'ANTERIOR', titulo:'Pendientes anteriores', citas:visibles.filter(c => c.periodoCobro === 'ANTERIOR')}
    ];
  }
  @Input({ required: true }) sucursalId!: number | null;
  @Input({ required: true }) citas!: CitaPorCobrar[];
  @Input({ required: true }) citaSeleccionadaId!: number | null;
  @Input({ required: true }) citaSeleccionada!: CitaPorCobrar | null;
  @Input({ required: true }) formularioPago!: FormularioPagoCaja;
  @Input({ required: true }) metodosPago!: OpcionCaja[];
  @Input({ required: true }) metodoPagoEfectivo!: string;
  @Input({ required: true }) pagos!: PagoCita[];
  @Input({ required: true }) guardando!: boolean;
  @Input({ required: true }) cajaAbierta!: boolean;
  @Input({ required: true }) mensajeBloqueo!: string;

  @Output() selectAppointment = new EventEmitter<number>();
  @Output() savePayment = new EventEmitter<void>();
  @Output() printReceipt = new EventEmitter<void>();
  @Output() pointPaymentCompleted = new EventEmitter<void>();
  @Output() pointStateChanged = new EventEmitter<void>();

  readonly terminalesPoint = signal<TerminalPointCaja[]>([]);
  private pointPoll?:Subscription;
  readonly accionPointEnCurso=signal(false);
  readonly ordenPoint = signal<OrdenPointCaja | null>(null);
  readonly procesandoPoint = signal(false);
  readonly simulandoPoint = signal(false);
  readonly errorPoint = signal('');
  terminalPointId: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['mostrarCobroInicial'] && this.mostrarCobroInicial) this.mostrandoCobro.set(true);
    if (changes['sucursalId']) this.cargarTerminalesPoint();
    if (changes['citaSeleccionada'] && changes['citaSeleccionada'].previousValue?.citaId!==changes['citaSeleccionada'].currentValue?.citaId && !this.procesandoPoint()) {
      this.adicionales=[];
      this.ordenPoint.set(null);
      this.errorPoint.set('');
    }
  }

  cobrarConPoint(s?:SolicitudCobro): void {
    const cita = this.citaSeleccionada;
    if (!cita || !this.sucursalId || !this.terminalPointId || this.guardando || this.procesandoPoint() || !this.cajaAbierta) return;
    this.procesandoPoint.set(true);
    this.errorPoint.set('');
    this.ordenPoint.set(null);
    this.caja.crearOrdenPoint(this.terminalPointId, {
      sucursalId: this.sucursalId,
      citaId: this.cobroMultiple ? null : cita.citaId,
      ...(this.cobroMultiple ? {citaIds:this.seleccionadas.map(c=>c.citaId)} : {}),
      clienteId: null,
      productos: [],
      ...(s?{pagos:s.pagos}:{metodoPago:this.terminalesPoint().find(t=>t.id===this.terminalPointId)?.metodoPago?.codigo}),
      referencia: null,
      observaciones: s?.observaciones || this.formularioPago.observaciones.trim() || null
    }, crypto.randomUUID()).subscribe({
      next: orden => {
        this.ordenPoint.set(orden);
        if (this.esEstadoFinalPoint(orden.estado)) this.finalizarPoint(orden);
        else this.vigilarPoint(orden.id);
      },
      error: error => {
        this.procesandoPoint.set(false);
        this.errorPoint.set(error?.error?.mensaje || error?.error?.detail || error?.error?.message || 'No fue posible enviar el cobro a la terminal.');
      }
    });
  }

  reintentarPoint():void {const o=this.ordenPoint();if(!o?.puedeReintentar || this.accionPointEnCurso())return;this.accionPointEnCurso.set(true);this.caja.reintentarOrdenPoint(o.id).pipe(finalize(()=>this.accionPointEnCurso.set(false))).subscribe({next:o=>{this.ordenPoint.set(o);if(this.esEstadoFinalPoint(o.estado))this.finalizarPoint(o);else this.vigilarPoint(o.id);},error:e=>this.errorPoint.set(e.error?.mensaje||'No se pudo consultar el mismo envío.')});}
  resolverPoint(r:ResolverPointPayload):void {const o=this.ordenPoint();if(!o?.puedeResolver || this.accionPointEnCurso())return;this.accionPointEnCurso.set(true);this.caja.resolverOrdenPoint(o.id,r).pipe(finalize(()=>this.accionPointEnCurso.set(false))).subscribe({next:o=>{this.ordenPoint.set(o);if(this.esEstadoFinalPoint(o.estado))this.finalizarPoint(o);else this.vigilarPoint(o.id);},error:e=>this.errorPoint.set(e.error?.mensaje||'No se pudo resolver la orden.')});}

  cancelarPoint(): void {
    const orden = this.ordenPoint();
    if (!orden || this.esEstadoFinalPoint(orden.estado)) return;
    this.caja.cancelarOrdenPoint(orden.id, crypto.randomUUID()).subscribe({
      next: actualizada => this.finalizarPoint(actualizada),
      error: error => this.errorPoint.set(error?.error?.mensaje || error?.error?.detail || 'No fue posible cancelar la orden Point.')
    });
  }

  simularAprobacionPoint(): void {
    const orden = this.ordenPoint();
    if (!orden?.simulacionDisponible || this.esEstadoFinalPoint(orden.estado) || this.simulandoPoint()) return;
    this.simulandoPoint.set(true); this.errorPoint.set('');
    this.caja.simularAprobacionPoint(orden.id).pipe(finalize(() => this.simulandoPoint.set(false))).subscribe({
      next: actualizada => this.ordenPoint.set(actualizada),
      error: error => this.errorPoint.set(error?.error?.mensaje || error?.error?.detail || 'No fue posible simular la aprobación de Point.')
    });
  }

  private cargarTerminalesPoint(): void {
    this.terminalesPoint.set([]);
    this.terminalPointId = null;
    if (!this.sucursalId) return;
    this.caja.listarTerminalesPoint(this.sucursalId).subscribe({
      next: terminales => {
        this.terminalesPoint.set(terminales);
        this.terminalPointId = terminales[0]?.id ?? null;
      },
      error: () => this.terminalesPoint.set([])
    });
  }

  private vigilarPoint(id: string): void {
    this.pointPoll?.unsubscribe();
    this.procesandoPoint.set(true);
    this.pointPoll=timer(2000, 3000).pipe(
      switchMap(() => this.caja.consultarOrdenPoint(id)),
      takeWhile(orden => !this.esEstadoFinalPoint(orden.estado) && orden.estado!=='REQUIERE_REVISION', true),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.procesandoPoint.set(false))
    ).subscribe({
      next: orden => {
        this.ordenPoint.set(orden);
        if (this.esEstadoFinalPoint(orden.estado)) this.finalizarPoint(orden);
      },
      error: error => this.errorPoint.set(error?.error?.mensaje || error?.error?.detail || 'No fue posible consultar la terminal.')
    });
  }

  private finalizarPoint(orden: OrdenPointCaja): void {
    this.pointPoll?.unsubscribe();
    this.ordenPoint.set(orden);
    this.procesandoPoint.set(false);
    if(orden.requiereRevision){this.errorPoint.set(orden.detalleEstado||'Pago aprobado con incidencia. Revisa la conciliación.');return;}
    if (orden.estado === 'APROBADO') {
      this.caja.comprobanteVenta(orden.ventaId).subscribe({next:v=>{this.ventaCobrada.set(v);this.adicionales=[];this.pointPaymentCompleted.emit();},error:()=>this.errorPoint.set('Pago aprobado. Consulta el comprobante en Actividad.')});
    } else {
      this.pointStateChanged.emit();
      const efectivo=Number(orden.efectivoPrevisto||0);
      this.errorPoint.set(`La tarjeta no se aprobó.${efectivo>0?' No se registró el efectivo de '+new Intl.NumberFormat('es-MX',{style:'currency',currency:orden.moneda,currencyDisplay:'narrowSymbol'}).format(efectivo)+': devuélvelo o cobra el total con otro método.':' Puedes cobrar el total con otro método.'}`);
    }
  }

  private esEstadoFinalPoint(estado: string): boolean {
    return ['APROBADO', 'CANCELADO', 'RECHAZADO', 'EXPIRADO', 'REEMBOLSADO'].includes(estado);
  }

  calcularCambio(): number {
    if (this.formularioPago.metodoPago !== this.metodoPagoEfectivo) {
      return 0;
    }
    const monto = Number(this.formularioPago.monto || 0);
    const recibido = Number(this.formularioPago.montoRecibido || 0);
    return Number.isFinite(monto) && Number.isFinite(recibido) && recibido > monto
      ? recibido - monto
      : 0;
  }
}
