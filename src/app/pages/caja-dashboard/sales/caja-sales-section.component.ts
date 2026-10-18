import {ResolverPointPayload} from '../../../core/caja/caja.service';
import {CajaCobroComponent,SolicitudCobro} from '../cobro/caja-cobro.component';
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Subscription,finalize, forkJoin, switchMap, takeWhile, timer } from 'rxjs';
import { CajaService, CitaPorCobrar, OpcionCaja, OrdenPointCaja, ProductoCaja, TerminalPointCaja, VentaCaja } from '../../../core/caja/caja.service';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';

interface LineaProducto { producto: ProductoCaja; cantidad: number; }

@Component({
  selector: 'app-caja-sales-section',
  standalone: true,
  imports: [CajaCobroComponent, CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MoneyDisplayPipe],
  templateUrl: './caja-sales-section.component.html',
  styleUrl: './caja-sales-section.component.css'
})
export class CajaSalesSectionComponent implements OnChanges {
  @Input({ required: true }) sucursalId!: number | null;
  @Input({ required: true }) citas!: CitaPorCobrar[];
  @Input({ required: true }) metodosPago!: OpcionCaja[];
  @Input({ required: true }) cajaAbierta!: boolean;
  @Output() saleCompleted = new EventEmitter<void>();
  @Output() pointStateChanged = new EventEmitter<void>();

  private readonly caja = inject(CajaService);
  private readonly destroyRef = inject(DestroyRef);
  readonly cuentaMovilAbierta=signal(false);
  readonly vistaPos=signal<'catalogo'|'staff'|'actividad'>('catalogo');
  cuentaStaff(){return this.pendientesStaff().find(p=>p.venta.id===this.ventaStaffSeleccionada)??null;}
  totalCuenta(){return Number(this.cuentaStaff()?.venta.total??this.totalEstimado());}
  monedaCuenta(){return this.cuentaStaff()?.venta.moneda??this.moneda();}
  cantidadConceptosCuenta(){return this.cuentaStaff()?.venta.detalles.length??this.conceptosSeleccionados();}
  seleccionarCuentaStaff(id:number){if(this.guardando())return;this.ventaStaffSeleccionada=id;this.verCuenta();}
  liberarCuentaStaff(){this.ventaStaffSeleccionada=null;this.vistaPos.set('catalogo');this.cuentaMovilAbierta.set(false);}
  abrirVistaPos(vista:'catalogo'|'staff'|'actividad'){if(this.guardando())return;if(vista==='catalogo')this.ventaStaffSeleccionada=null;this.vistaPos.set(vista);this.cuentaMovilAbierta.set(false);}
  readonly productos = signal<ProductoCaja[]>([]);
  readonly ventas = signal<VentaCaja[]>([]);
  readonly comprobante = signal<VentaCaja | null>(null);
  readonly pendientesStaff = signal<{venta:VentaCaja;clienteNombre:string;clienteTelefono:string}[]>([]);
  ventaStaffSeleccionada: number | null = null;
  paginaStaff=0; readonly paginasStaff=signal(0);readonly totalStaff=signal(0);
  moverPaginaStaff(delta:number){if(this.guardando())return;this.paginaStaff+=delta;this.ventaStaffSeleccionada=null;this.cargar();}
  readonly terminalesPoint = signal<TerminalPointCaja[]>([]);
  private pointPoll?:Subscription;
  readonly accionPointEnCurso=signal(false);
  readonly ordenPoint = signal<OrdenPointCaja | null>(null);
  readonly historialPoint = signal<OrdenPointCaja[]>([]);
  readonly lineas = signal<LineaProducto[]>([]);
  readonly citaIds = signal<number[]>([]);
  private readonly citasVersion = signal(0);
  readonly busqueda = signal('');
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly simulandoPoint = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  metodoPago = '';
  terminalPointId: number | null = null;
  referencia = '';
  observaciones = '';

  readonly productosFiltrados = computed(() => {
    const termino = this.busqueda().trim().toLocaleLowerCase('es-MX');
    return this.productos().filter(item => !termino || `${item.nombre} ${item.sku} ${item.codigoBarras || ''}`.toLocaleLowerCase('es-MX').includes(termino));
  });
  readonly citasSeleccionadas = computed(() => {this.citasVersion(); return this.citas.filter(c => this.citaIds().includes(c.citaId));});
  readonly citaSeleccionada = computed(() => this.citasSeleccionadas()[0] ?? null);
  compatible(c: CitaPorCobrar) {const primera=this.citaSeleccionada(); return !primera || (primera.clienteId === c.clienteId && primera.sucursalId === c.sucursalId && primera.moneda === c.moneda); }
  readonly cotizacion = signal<{ subtotal: number; descuento: number; total: number; lineas: { productoId: number; precioUnitario: number; total: number }[] } | null>(null);
  readonly cotizando = signal(false);
  readonly subtotalProductos = computed(() => Number(this.cotizacion()?.total ?? 0));
  constructor() {
    effect(onCleanup => {
      const lineas = this.lineas();
      this.cotizacion.set(null);
      if (!lineas.length || !this.sucursalId) { this.cotizando.set(false); return; }
      this.cotizando.set(true);
      const subscription = this.caja.cotizarProductos(this.sucursalId, lineas.map(item => ({ productoId: item.producto.id, cantidad: item.cantidad }))).subscribe({
        next: resultado => { this.cotizacion.set(resultado); this.cotizando.set(false); },
        error: () => { this.cotizando.set(false); this.error.set('No fue posible cotizar los productos. Cambia la cantidad para reintentar.'); }
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }
  readonly totalEstimado = computed(() => this.subtotalProductos() + this.citasSeleccionadas().reduce((total,c) => total + Number(c.pendiente),0));
  readonly conceptosSeleccionados = computed(() => this.lineas().reduce((total, item) => total + item.cantidad, 0) + this.citasSeleccionadas().length);
  readonly moneda = computed(() => this.citaSeleccionada()?.moneda ?? this.lineas()[0]?.producto.moneda ?? 'MXN');

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['citas']) {this.citasVersion.update(v=>v+1); this.citaIds.update(ids=>ids.filter(id=>this.citas.some(c=>c.citaId===id)));}
    if (changes['metodosPago'] && !this.metodosPago.some(item => item.codigo === this.metodoPago)) {
      this.metodoPago = this.metodosPago[0]?.codigo ?? '';
    }
    if (changes['sucursalId']) {
      this.cuentaMovilAbierta.set(false);this.vistaPos.set('catalogo');
      this.ventaStaffSeleccionada=null;this.paginaStaff=0;this.pendientesStaff.set([]);this.totalStaff.set(0);this.paginasStaff.set(0);
      this.cotizacion.set(null); this.lineas.set([]); this.citaIds.set([]);
      if (this.sucursalId) this.cargar();
    }
  }

  seleccionarCita(id: number | null): void {
    if(this.guardando() || id === null) return;
    const cita=this.citas.find(c=>c.citaId === id); if (!cita || !this.compatible(cita)) return;
    this.citaIds.update(ids=>ids.includes(id)?ids.filter(i=>i!==id):[...ids,id]);
  }
  cantidad(productoId: number): number { return this.lineas().find(item => item.producto.id === productoId)?.cantidad ?? 0; }
  precioCotizado(productoId: number) { return this.cotizacion()?.lineas.find(item => item.productoId === productoId); }
  agregar(producto: ProductoCaja): void {
    if(this.guardando())return;
    if (producto.controlaInventario && Number(producto.disponible ?? 0) <= this.cantidad(producto.id)) return;
    this.cotizacion.set(null);
    this.lineas.update(items => {
      const existente = items.find(item => item.producto.id === producto.id);
      return existente ? items.map(item => item.producto.id === producto.id ? { ...item, cantidad: item.cantidad + 1 } : item)
        : [...items, { producto, cantidad: 1 }];
    });
  }
  disminuir(productoId: number): void {
    if(this.guardando())return;
    this.cotizacion.set(null);
    this.lineas.update(items => items.flatMap(item => item.producto.id !== productoId ? [item] : item.cantidad > 1 ? [{ ...item, cantidad: item.cantidad - 1 }] : []));
  }
  quitar(productoId: number): void { if(this.guardando())return;this.cotizacion.set(null); this.lineas.update(items => items.filter(item => item.producto.id !== productoId)); }

  verCuenta(): void {
    this.cuentaMovilAbierta.set(true);
    requestAnimationFrame(() => document.getElementById('caja-resumen-venta')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  get bloqueoPanel(){return !this.cajaAbierta?'Abre la caja de esta sucursal para cobrar.':this.citasSeleccionadas().some(c=>c.cobroPointEnCurso)?'Esta cuenta tiene un cobro Point en curso. Retómalo en Actividad.':this.cotizando()?'Calculando el total…':!this.cuentaStaff() && this.lineas().length && !this.cotizacion()?'Espera la cotización del servidor.':'';}
  confirmarPanel(s:SolicitudCobro){
    if(this.guardando() || this.bloqueoPanel || !this.sucursalId)return;
    const payload={sucursalId:this.sucursalId,citaId:null,citaIds:this.citaIds(),clienteId:null,productos:this.lineas().map(l=>({productoId:l.producto.id,cantidad:l.cantidad})),pagos:s.pagos,referencia:null,observaciones:s.observaciones||null,...(this.ventaStaffSeleccionada?{ventaPendienteId:this.ventaStaffSeleccionada}: {})};
    this.guardando.set(true);this.error.set('');this.mensaje.set('');this.ordenPoint.set(null);
    if(s.usaPoint && s.terminalId){this.caja.crearOrdenPoint(s.terminalId,payload,crypto.randomUUID()).subscribe({next:o=>{this.ordenPoint.set(o);if(this.esEstadoFinalPoint(o.estado))this.finalizarPoint(o);else this.vigilarPoint(o.id);},error:e=>{this.guardando.set(false);this.error.set(e.error?.mensaje||e.error?.detail||'No se pudo enviar a la terminal.');}});return;}
    this.caja.registrarVenta(payload).pipe(finalize(()=>this.guardando.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({next:v=>{this.comprobante.set(v);this.lineas.set([]);this.citaIds.set([]);this.ventaStaffSeleccionada=null;this.saleCompleted.emit();this.cargar();},error:e=>this.error.set(e.error?.mensaje||e.error?.detail||'No se pudo cobrar la cuenta.')});
  }
  private finalizarPoint(o:OrdenPointCaja){
    this.pointPoll?.unsubscribe();
    this.ordenPoint.set(o);this.guardando.set(false);
    if(o.requiereRevision){this.error.set(o.detalleEstado||'Pago aprobado con incidencia. Revisa la conciliación.');return;}
    if(o.estado==='APROBADO'){
      this.caja.comprobanteVenta(o.ventaId).subscribe({next:v=>{this.comprobante.set(v);this.lineas.set([]);this.citaIds.set([]);this.ventaStaffSeleccionada=null;this.saleCompleted.emit();this.cargar();},error:()=>this.error.set('Pago aprobado. Consulta el comprobante en Actividad.')});
    }else{
      this.pointStateChanged.emit();
      const efectivo=Number(o.efectivoPrevisto||0);
      this.error.set('La tarjeta no se aprobó.'+(efectivo>0?' No se registró el efectivo de '+new Intl.NumberFormat('es-MX',{style:'currency',currency:o.moneda,currencyDisplay:'narrowSymbol'}).format(efectivo)+': devuélvelo o cobra el total con otro método.':' Puedes cobrar el total con otro método.'));
    }
  }

  reintentarPoint():void {const o=this.ordenPoint();if(!o?.puedeReintentar || this.accionPointEnCurso())return;this.accionPointEnCurso.set(true);this.caja.reintentarOrdenPoint(o.id).pipe(finalize(()=>this.accionPointEnCurso.set(false))).subscribe({next:o=>{this.ordenPoint.set(o);if(this.esEstadoFinalPoint(o.estado))this.finalizarPoint(o);else this.vigilarPoint(o.id);},error:e=>this.error.set(e.error?.mensaje||'No se pudo consultar el mismo envío.')});}
  resolverPoint(r:ResolverPointPayload):void {const o=this.ordenPoint();if(!o?.puedeResolver || this.accionPointEnCurso())return;this.accionPointEnCurso.set(true);this.caja.resolverOrdenPoint(o.id,r).pipe(finalize(()=>this.accionPointEnCurso.set(false))).subscribe({next:o=>{this.ordenPoint.set(o);if(this.esEstadoFinalPoint(o.estado))this.finalizarPoint(o);else this.vigilarPoint(o.id);},error:e=>this.error.set(e.error?.mensaje||'No se pudo resolver la orden.')});}

  cancelarPoint(): void {
    const orden = this.ordenPoint();
    if (!orden || this.esEstadoFinalPoint(orden.estado)) return;
    this.caja.cancelarOrdenPoint(orden.id, crypto.randomUUID()).subscribe({
      next: actualizada => this.finalizarPoint(actualizada),
      error: error => this.error.set(error?.error?.mensaje || error?.error?.detail || 'No fue posible cancelar la orden Point.')
    });
  }

  continuarOrdenPoint(orden: OrdenPointCaja): void {
    if ((this.esEstadoFinalPoint(orden.estado) && !orden.requiereRevision) || this.guardando()) return;
    this.vistaPos.set('catalogo');this.cuentaMovilAbierta.set(true);
    this.ordenPoint.set(orden); this.guardando.set(true); this.error.set('');
    if(this.esEstadoFinalPoint(orden.estado)){this.guardando.set(false);return;}
    this.mensaje.set('Cobro pendiente recuperado. Completa la operación en la terminal o usa la simulación de pruebas.');
    this.vigilarPoint(orden.id);
  }

  simularAprobacionPoint(): void {
    const orden = this.ordenPoint();
    if (!orden?.simulacionDisponible || this.esEstadoFinalPoint(orden.estado) || this.simulandoPoint()) return;
    this.simulandoPoint.set(true); this.error.set('');
    this.caja.simularAprobacionPoint(orden.id).pipe(finalize(() => this.simulandoPoint.set(false))).subscribe({
      next: actualizada => {
        this.ordenPoint.set(actualizada);
        this.mensaje.set('Mercado Pago recibió el pago simulado. Estamos conciliando la venta…');
      },
      error: error => this.error.set(error?.error?.mensaje || error?.error?.detail || 'No fue posible simular la aprobación de Point.')
    });
  }

  reembolsarPoint(orden: OrdenPointCaja): void {
    if (orden.estado !== 'APROBADO' || !window.confirm(`¿Reembolsar el cobro de la venta ${orden.folioVenta}?`)) return;
    this.guardando.set(true); this.error.set(''); this.mensaje.set('');
    this.caja.reembolsarOrdenPoint(orden.id, crypto.randomUUID()).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: actualizada => {
        this.historialPoint.update(items => items.map(item => item.id === actualizada.id ? actualizada : item));
        this.mensaje.set(`El cobro de ${actualizada.folioVenta} fue reembolsado y conciliado.`); this.cargar();
      },
      error: error => this.error.set(error?.error?.mensaje || error?.error?.detail || 'No fue posible reembolsar el cobro Point.')
    });
  }

  private vigilarPoint(id: string): void {
    this.pointPoll?.unsubscribe();
    this.guardando.set(true);
    this.pointPoll=timer(2000, 3000).pipe(
      switchMap(() => this.caja.consultarOrdenPoint(id)),
      takeWhile(orden => !this.esEstadoFinalPoint(orden.estado) && orden.estado!=='REQUIERE_REVISION', true),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.guardando.set(false))
    ).subscribe({
      next: orden => {
        this.ordenPoint.set(orden);
        if(this.esEstadoFinalPoint(orden.estado))this.finalizarPoint(orden);
      },
      error: error => { this.guardando.set(false); this.error.set(error?.error?.mensaje || error?.error?.detail || 'No fue posible consultar el estado de la terminal.') }
    });
  }

  private esEstadoFinalPoint(estado: string): boolean {
    return ['APROBADO', 'CANCELADO', 'RECHAZADO', 'EXPIRADO', 'REEMBOLSADO'].includes(estado);
  }

  private cargar(): void {
    if (!this.sucursalId) return;
    this.cargando.set(true); this.error.set('');
    forkJoin({ pendientesStaff: this.caja.ventasPendientesStaff(this.sucursalId,this.paginaStaff), catalogo: this.caja.getCatalogoVenta(this.sucursalId), ventas: this.caja.listarVentas(this.sucursalId), terminales: this.caja.listarTerminalesPoint(this.sucursalId), ordenesPoint: this.caja.listarOrdenesPoint(this.sucursalId) })
      .pipe(finalize(() => this.cargando.set(false))).subscribe({
        next: ({ pendientesStaff, catalogo, ventas, terminales, ordenesPoint }) => {
          this.pendientesStaff.set(pendientesStaff.content);this.paginasStaff.set(pendientesStaff.totalPages);this.totalStaff.set(pendientesStaff.totalElements); this.productos.set(catalogo.productos ?? []); this.ventas.set(ventas ?? []); this.terminalesPoint.set(terminales ?? []); this.historialPoint.set(ordenesPoint ?? []);
          if (!terminales.some(item => item.id === this.terminalPointId)) this.terminalPointId = terminales[0]?.id ?? null;
        },
        error: error => this.error.set(error?.error?.mensaje || error?.error?.detail || 'No fue posible cargar el punto de venta.')
      });
  }
}
