import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
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
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MoneyDisplayPipe],
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
  readonly productos = signal<ProductoCaja[]>([]);
  readonly ventas = signal<VentaCaja[]>([]);
  readonly terminalesPoint = signal<TerminalPointCaja[]>([]);
  readonly ordenPoint = signal<OrdenPointCaja | null>(null);
  readonly historialPoint = signal<OrdenPointCaja[]>([]);
  readonly lineas = signal<LineaProducto[]>([]);
  readonly citaId = signal<number | null>(null);
  readonly busqueda = signal('');
  readonly cargando = signal(false);
  readonly guardando = signal(false);
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
  readonly citaSeleccionada = computed(() => this.citas.find(item => item.citaId === this.citaId()) ?? null);
  readonly subtotalProductos = computed(() => this.lineas().reduce((total, item) => total + item.producto.precio * item.cantidad, 0));
  readonly totalEstimado = computed(() => this.subtotalProductos() + Number(this.citaSeleccionada()?.pendiente ?? 0));
  readonly moneda = computed(() => this.citaSeleccionada()?.moneda ?? this.lineas()[0]?.producto.moneda ?? 'MXN');

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['metodosPago'] && !this.metodosPago.some(item => item.codigo === this.metodoPago)) {
      this.metodoPago = this.metodosPago[0]?.codigo ?? '';
    }
    if (changes['sucursalId'] && this.sucursalId) this.cargar();
  }

  seleccionarCita(id: number | null): void { this.citaId.set(this.citaId() === id ? null : id); }
  cantidad(productoId: number): number { return this.lineas().find(item => item.producto.id === productoId)?.cantidad ?? 0; }
  agregar(producto: ProductoCaja): void {
    if (producto.controlaInventario && Number(producto.disponible ?? 0) <= this.cantidad(producto.id)) return;
    this.lineas.update(items => {
      const existente = items.find(item => item.producto.id === producto.id);
      return existente ? items.map(item => item.producto.id === producto.id ? { ...item, cantidad: item.cantidad + 1 } : item)
        : [...items, { producto, cantidad: 1 }];
    });
  }
  disminuir(productoId: number): void {
    this.lineas.update(items => items.flatMap(item => item.producto.id !== productoId ? [item] : item.cantidad > 1 ? [{ ...item, cantidad: item.cantidad - 1 }] : []));
  }
  quitar(productoId: number): void { this.lineas.update(items => items.filter(item => item.producto.id !== productoId)); }

  cobrar(): void {
    if (!this.sucursalId || (!this.citaId() && !this.lineas().length) || !this.metodoPago || !this.cajaAbierta) return;
    if (this.metodoPago === 'MERCADO_PAGO_POINT') { this.cobrarConPoint(); return; }
    this.guardando.set(true); this.error.set(''); this.mensaje.set('');
    this.caja.registrarVenta({
      sucursalId: this.sucursalId, citaId: this.citaId(), clienteId: null,
      productos: this.lineas().map(item => ({ productoId: item.producto.id, cantidad: item.cantidad })),
      metodoPago: this.metodoPago, referencia: this.referencia.trim() || null, observaciones: this.observaciones.trim() || null
    }).pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: venta => {
        this.lineas.set([]); this.citaId.set(null); this.referencia = ''; this.observaciones = '';
        this.mensaje.set(`Venta ${venta.folio} cobrada correctamente.`); this.ventas.update(items => [venta, ...items]);
        this.saleCompleted.emit(); this.cargar();
      },
      error: error => this.error.set(error?.error?.detail || error?.error?.message || 'No fue posible registrar la venta.')
    });
  }

  cobrarConPoint(): void {
    if (!this.sucursalId || !this.terminalPointId || this.guardando()) return;
    this.guardando.set(true); this.error.set(''); this.mensaje.set(''); this.ordenPoint.set(null);
    const venta = this.payloadVenta('MERCADO_PAGO_POINT');
    this.caja.crearOrdenPoint(this.terminalPointId, venta, crypto.randomUUID()).subscribe({
      next: orden => { this.ordenPoint.set(orden); this.mensaje.set('La cuenta fue enviada a la terminal. Esperando el pago…'); this.vigilarPoint(orden.id); },
      error: error => { this.guardando.set(false); this.error.set(error?.error?.detail || error?.error?.message || 'No fue posible enviar el cobro a la terminal.') }
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
          this.lineas.set([]); this.citaId.set(null); this.referencia = ''; this.observaciones = '';
          this.mensaje.set(`Pago aprobado en ${orden.terminalNombre}. Venta ${orden.folioVenta} conciliada.`);
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
      sucursalId: this.sucursalId!, citaId: this.citaId(), clienteId: null,
      productos: this.lineas().map(item => ({ productoId: item.producto.id, cantidad: item.cantidad })),
      metodoPago, referencia: this.referencia.trim() || null, observaciones: this.observaciones.trim() || null
    };
  }

  private cargar(): void {
    if (!this.sucursalId) return;
    this.cargando.set(true); this.error.set('');
    forkJoin({ catalogo: this.caja.getCatalogoVenta(this.sucursalId), ventas: this.caja.listarVentas(this.sucursalId), terminales: this.caja.listarTerminalesPoint(this.sucursalId), ordenesPoint: this.caja.listarOrdenesPoint(this.sucursalId) })
      .pipe(finalize(() => this.cargando.set(false))).subscribe({
        next: ({ catalogo, ventas, terminales, ordenesPoint }) => {
          this.productos.set(catalogo.productos ?? []); this.ventas.set(ventas ?? []); this.terminalesPoint.set(terminales ?? []); this.historialPoint.set(ordenesPoint ?? []);
          if (!terminales.some(item => item.id === this.terminalPointId)) this.terminalPointId = terminales[0]?.id ?? null;
        },
        error: error => this.error.set(error?.error?.detail || 'No fue posible cargar el punto de venta.')
      });
  }
}
