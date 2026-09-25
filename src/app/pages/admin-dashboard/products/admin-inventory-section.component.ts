import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { InventoryAdminService, CatalogoInventario, ExistenciaProducto, MovimientoInventario } from './inventory-admin.service';

type PanelInventario = 'ajuste' | 'transferencia' | null;

@Component({
  selector: 'app-admin-inventory-section',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  providers: [InventoryAdminService],
  templateUrl: './admin-inventory-section.component.html',
  styleUrl: './admin-inventory-section.component.css'
})
export class AdminInventorySectionComponent implements OnInit {
  private readonly service = inject(InventoryAdminService);
  readonly catalogo = signal<CatalogoInventario | null>(null);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly busqueda = signal('');
  readonly sucursalFiltro = signal<number | null>(null);
  readonly panel = signal<PanelInventario>(null);

  ajuste = { productoId: null as number | null, sucursalId: null as number | null, tipo: 'ENTRADA', cantidad: 1, referencia: '', notas: '' };
  transferencia = { productoId: null as number | null, sucursalOrigenId: null as number | null, sucursalDestinoId: null as number | null, cantidad: 1, referencia: '', notas: '' };

  readonly existenciasFiltradas = computed(() => {
    const termino = this.busqueda().trim().toLocaleLowerCase('es-MX');
    const sucursalId = this.sucursalFiltro();
    return (this.catalogo()?.existencias ?? []).filter(item =>
      (!sucursalId || item.sucursalId === sucursalId) &&
      (!termino || [item.productoNombre, item.sku, item.sucursalNombre].some(valor => valor.toLocaleLowerCase('es-MX').includes(termino)))
    );
  });
  readonly totalUnidades = computed(() => this.existenciasFiltradas().reduce((total, item) => total + Number(item.disponible), 0));
  readonly bajoStock = computed(() => this.existenciasFiltradas().filter(item => item.estado === 'BAJO').length);
  readonly agotados = computed(() => this.existenciasFiltradas().filter(item => item.estado === 'AGOTADO').length);
  readonly movimientosVisibles = computed(() => {
    const sucursalId = this.sucursalFiltro();
    return (this.catalogo()?.movimientos ?? []).filter(item => !sucursalId || item.sucursalId === sucursalId).slice(0, 12);
  });

  ngOnInit(): void { this.cargar(); }

  abrirPanel(panel: Exclude<PanelInventario, null>, existencia?: ExistenciaProducto): void {
    this.panel.set(this.panel() === panel ? null : panel);
    if (existencia) {
      if (panel === 'ajuste') { this.ajuste.productoId = existencia.productoId; this.ajuste.sucursalId = existencia.sucursalId; }
      else { this.transferencia.productoId = existencia.productoId; this.transferencia.sucursalOrigenId = existencia.sucursalId; }
    }
    this.limpiarMensajes();
  }

  guardarAjuste(): void {
    const cantidadInvalida = this.ajuste.tipo === 'AJUSTE' ? this.ajuste.cantidad < 0 : this.ajuste.cantidad <= 0;
    if (!this.ajuste.productoId || !this.ajuste.sucursalId || !this.ajuste.tipo || cantidadInvalida || this.guardando()) return;
    this.guardando.set(true); this.limpiarMensajes();
    this.service.ajustar({
      productoId: this.ajuste.productoId!,
      sucursalId: this.ajuste.sucursalId!,
      tipo: this.ajuste.tipo,
      cantidad: this.ajuste.cantidad,
      referencia: this.ajuste.referencia.trim() || null,
      notas: this.ajuste.notas.trim() || null
    })
      .pipe(finalize(() => this.guardando.set(false))).subscribe({
        next: () => { this.panel.set(null); this.cargar('Movimiento registrado correctamente.'); },
        error: error => this.mostrarError(error, 'No fue posible registrar el movimiento.')
      });
  }

  guardarTransferencia(): void {
    if (!this.transferencia.productoId || !this.transferencia.sucursalOrigenId || !this.transferencia.sucursalDestinoId ||
        this.transferencia.sucursalOrigenId === this.transferencia.sucursalDestinoId || this.transferencia.cantidad <= 0 || this.guardando()) return;
    this.guardando.set(true); this.limpiarMensajes();
    this.service.transferir({
      productoId: this.transferencia.productoId!,
      sucursalOrigenId: this.transferencia.sucursalOrigenId!,
      sucursalDestinoId: this.transferencia.sucursalDestinoId!,
      cantidad: this.transferencia.cantidad,
      referencia: this.transferencia.referencia.trim() || null,
      notas: this.transferencia.notas.trim() || null
    })
      .pipe(finalize(() => this.guardando.set(false))).subscribe({
        next: () => { this.panel.set(null); this.cargar('Transferencia registrada correctamente.'); },
        error: error => this.mostrarError(error, 'No fue posible realizar la transferencia.')
      });
  }

  etiquetaMovimiento(tipo: string): string {
    return ({ ENTRADA: 'Entrada', SALIDA: 'Salida', AJUSTE: 'Conteo físico', TRANSFERENCIA_ENTRADA: 'Transferencia recibida', TRANSFERENCIA_SALIDA: 'Transferencia enviada' } as Record<string, string>)[tipo] ?? tipo;
  }
  esIncremento(movimiento: MovimientoInventario): boolean { return Number(movimiento.cantidad) >= 0; }

  private cargar(mensaje?: string): void {
    this.cargando.set(true);
    this.service.obtenerCatalogo().pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: catalogo => { this.catalogo.set(catalogo); if (mensaje) this.mensaje.set(mensaje); },
      error: error => this.mostrarError(error, 'No fue posible cargar el inventario.')
    });
  }
  private mostrarError(error: any, predeterminado: string): void { this.error.set(error?.error?.message || error?.error?.detail || predeterminado); }
  private limpiarMensajes(): void { this.error.set(null); this.mensaje.set(null); }
}
