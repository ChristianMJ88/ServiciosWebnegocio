import { VentaComprobanteComponent } from '../../../shared/components/venta-comprobante.component';
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { finalize, forkJoin, switchMap, takeWhile, timer } from 'rxjs';
import { CajaService, CitaPorCobrar, OpcionCaja, OrdenPointCaja, ProductoCaja, TerminalPointCaja, VentaCaja } from '../../../core/caja/caja.service';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';

interface LineaProducto { producto: ProductoCaja; cantidad: number; }

@Component({
  selector: 'app-caja-sales-section',
  standalone: true,
  imports: [VentaComprobanteComponent, CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MoneyDisplayPipe],
  templateUrl: './caja-sales-section.component.html',
  styleUrl: './caja-sales-section.component.css'
})
export class CajaSalesSectionComponent implements OnChanges {
  @Input({ required: true }) sucursalId!: number | null;
  @Input({ required: true }) citas!: CitaPorCobrar[];
  @Input({ required: true }) metodosPago!: OpcionCaja[];
  @Input({ required: true }) cajaAbierta!: boolean;
  @Output() saleCompleted = new EventEmitter<void>();

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
  puedeCobrarCuenta(){
    if(this.guardando()||!this.cajaAbierta||!this.metodoPago||(this.metodoPago==='MERCADO_PAGO_POINT'&&!this.terminalPointId))return false;
    if(this.cuentaStaff())return true;
    return !this.cotizando()&&(!this.lineas().length||!!this.cotizacion())&&!!(this.citaSeleccionada()||this.lineas().length);
  }
  readonly productos = signal<ProductoCaja[]>([]);
  readonly ventas = signal<VentaCaja[]>([]);
  readonly comprobante = signal<VentaCaja | null>(null);
  readonly pendientesStaff = signal<{venta:VentaCaja;clienteNombre:string;clienteTelefono:string}[]>([]);
  ventaStaffSeleccionada: number | null = null;
  paginaStaff=0; readonly paginasStaff=signal(0);readonly totalStaff=signal(0);
  moverPaginaStaff(delta:number){if(this.guardando())return;this.paginaStaff+=delta;this.ventaStaffSeleccionada=null;this.cargar();}
  readonly terminalesPoint = signal<TerminalPointCaja[]>([]);
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

  cobrarCuentaStaff():void {
    if(!this.ventaStaffSeleccionada || !this.cajaAbierta || !this.metodoPago || this.guardando())return;
    if(this.metodoPago==='MERCADO_PAGO_POINT'){this.cobrarCuentaStaffPoint();return;}
    this.guardando.set(true);this.error.set('');
    this.caja.cobrarVentaStaff(this.ventaStaffSeleccionada,this.metodoPago,this.referencia.trim()||null)
      .pipe(finalize(()=>this.guardando.set(false)),takeUntilDestroyed(this.destroyRef)).subscribe({
        next:v=>{this.ventaStaffSeleccionada=null;this.mensaje.set(`Cuenta ${v.folio} cobrada.`);this.saleCompleted.emit();this.cargar();},
        error:e=>this.error.set(e.error?.detail||'No se pudo cobrar la cuenta.')
      });
  }

  cobrarCuentaStaffPoint():void {
    if(!this.terminalPointId||!this.sucursalId||!this.ventaStaffSeleccionada)return;
    this.guardando.set(true);this.error.set('');this.ordenPoint.set(null);
    this.caja.crearOrdenPoint(this.terminalPointId,{sucursalId:this.sucursalId,citaId:null,clienteId:null,productos:[],metodoPago:'MERCADO_PAGO_POINT',referencia:null,observaciones:null,ventaPendienteId:this.ventaStaffSeleccionada},crypto.randomUUID()).subscribe({
      next:o=>{this.ordenPoint.set(o);this.mensaje.set('Cuenta enviada a la terminal. Esperando el pago.');this.vigilarPoint(o.id);},
      error:e=>{this.guardando.set(false);this.error.set(e.error?.detail||'No se pudo enviar a la terminal.');}
    });
  }

  cobrar(): void {
    if (!this.sucursalId || this.cotizando() || (this.lineas().length && !this.cotizacion()) || (!this.citaIds().length && !this.lineas().length) || !this.metodoPago || !this.cajaAbierta) return;
    if (this.metodoPago === 'MERCADO_PAGO_POINT') { this.cobrarConPoint(); return; }
    this.guardando.set(true); this.error.set(''); this.mensaje.set('');
    this.caja.registrarVenta({
      sucursalId: this.sucursalId, citaId: null, citaIds: this.citaIds(), clienteId: null,
      productos: this.lineas().map(item => ({ productoId: item.producto.id, cantidad: item.cantidad })),
      metodoPago: this.metodoPago, referencia: this.referencia.trim() || null, observaciones: this.observaciones.trim() || null
    }).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: venta => {
        this.comprobante.set(venta);
        this.lineas.set([]); this.citaIds.set([]); this.referencia = ''; this.observaciones = '';
        this.mensaje.set(`Venta ${venta.folio} cobrada correctamente.`); this.ventas.update(items => [venta, ...items]);
        this.saleCompleted.emit(); this.cargar();
      },
      error: error => this.error.set(error?.error?.mensaje || error?.error?.detail || error?.error?.message || 'No fue posible registrar la venta.')
    });
  }

  cobrarConPoint(): void {
    if (!this.sucursalId || !this.terminalPointId || this.guardando() || this.cotizando() || (this.lineas().length && !this.cotizacion())) return;
    this.guardando.set(true); this.error.set(''); this.mensaje.set(''); this.ordenPoint.set(null);
    const venta = this.payloadVenta('MERCADO_PAGO_POINT');
    this.caja.crearOrdenPoint(this.terminalPointId, venta, crypto.randomUUID()).subscribe({
      next: orden => { this.ordenPoint.set(orden); this.mensaje.set('La cuenta fue enviada a la terminal. Esperando el pago…'); this.vigilarPoint(orden.id); },
      error: error => { this.guardando.set(false); this.error.set(error?.error?.mensaje || error?.error?.detail || error?.error?.message || 'No fue posible enviar el cobro a la terminal.') }
    });
  }

  cancelarPoint(): void {
    const orden = this.ordenPoint();
    if (!orden || this.esEstadoFinalPoint(orden.estado)) return;
    this.caja.cancelarOrdenPoint(orden.id, crypto.randomUUID()).subscribe({
      next: actualizada => { this.ordenPoint.set(actualizada); this.guardando.set(false); this.mensaje.set('La orden Point fue cancelada.'); this.cargar(); },
      error: error => this.error.set(error?.error?.detail || 'No fue posible cancelar la orden Point.')
    });
  }

  continuarOrdenPoint(orden: OrdenPointCaja): void {
    if (this.esEstadoFinalPoint(orden.estado) || this.guardando()) return;
    this.ordenPoint.set(orden); this.guardando.set(true); this.error.set('');
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
      error: error => this.error.set(error?.error?.detail || 'No fue posible simular la aprobación de Point.')
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
      error: error => this.error.set(error?.error?.detail || 'No fue posible reembolsar el cobro Point.')
    });
  }

  private vigilarPoint(id: string): void {
    timer(2000, 3000).pipe(
      switchMap(() => this.caja.consultarOrdenPoint(id)),
      takeWhile(orden => !this.esEstadoFinalPoint(orden.estado), true),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.guardando.set(false))
    ).subscribe({
      next: orden => {
        this.ordenPoint.set(orden);
        if (orden.estado === 'APROBADO') {
          this.lineas.set([]); this.citaIds.set([]); this.referencia = ''; this.observaciones = '';
          this.ventaStaffSeleccionada=null; this.mensaje.set(`Pago aprobado en ${orden.terminalNombre}. Venta ${orden.folioVenta} conciliada.`);
          this.saleCompleted.emit(); this.cargar();
        } else if (this.esEstadoFinalPoint(orden.estado)) {
          this.error.set(`El cobro Point terminó con estado ${orden.estado.toLowerCase()}.`); this.cargar();
        }
      },
      error: error => { this.guardando.set(false); this.error.set(error?.error?.detail || 'No fue posible consultar el estado de la terminal.') }
    });
  }

  private esEstadoFinalPoint(estado: string): boolean {
    return ['APROBADO', 'CANCELADO', 'RECHAZADO', 'EXPIRADO', 'REEMBOLSADO'].includes(estado);
  }

  private payloadVenta(metodoPago: string) {
    return {
      sucursalId: this.sucursalId!, citaId: null, citaIds: this.citaIds(), clienteId: null,
      productos: this.lineas().map(item => ({ productoId: item.producto.id, cantidad: item.cantidad })),
      metodoPago, referencia: this.referencia.trim() || null, observaciones: this.observaciones.trim() || null
    };
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
        error: error => this.error.set(error?.error?.detail || 'No fue posible cargar el punto de venta.')
      });
  }
}
