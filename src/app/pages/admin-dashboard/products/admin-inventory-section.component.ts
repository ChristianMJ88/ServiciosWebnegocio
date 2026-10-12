import { A11yModule } from '@angular/cdk/a11y';
import { DOCUMENT } from '@angular/common';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import {
  InventoryAdminService,
  CatalogoInventario,
  ExistenciaProducto,
  MovimientoInventario,
  SucursalInventario,
  ProductoInventario,
  EstadoInventario
} from './inventory-admin.service';

export type TabInventario = 'existencias' | 'movimientos' | 'reposicion';
export type PanelInventario = 'ajuste' | 'transferencia' | 'kardex' | null;
export type TipoFiltroEstado = 'TODOS' | 'DISPONIBLE' | 'BAJO' | 'AGOTADO' | 'CON_RESERVA';
export type TipoOrdenInventario = 'urgente' | 'disponible_desc' | 'disponible_asc' | 'nombre_asc' | 'nombre_desc' | 'sku';
export type ModoVisualizacion = 'grid' | 'table';

export interface ItemReposicion extends ExistenciaProducto {
  unidadesFaltantes: number;
  sugeridoReposicion: number;
}

@Component({
  selector: 'app-admin-inventory-section',
  standalone: true,
  imports: [A11yModule, CommonModule, FormsModule, DecimalPipe, DatePipe],
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

  // Navegación y Vistas
  readonly tabActivo = signal<TabInventario>('existencias');
  readonly modoVista = signal<ModoVisualizacion>('grid');

  // Filtros de existencias
  readonly busqueda = signal('');
  readonly sucursalFiltro = signal<number | null>(null);
  readonly estadoFiltro = signal<TipoFiltroEstado>('TODOS');
  readonly ordenFiltro = signal<TipoOrdenInventario>('urgente');

  // Filtros de movimientos
  readonly busquedaMovimientos = signal('');
  readonly tipoMovimientoFiltro = signal<string | null>(null);
  readonly sucursalMovimientoFiltro = signal<number | null>(null);

  // Paneles / Modales
  readonly panel = signal<PanelInventario>(null);
  readonly productoKardex = signal<ExistenciaProducto | null>(null);

  private readonly document = inject(DOCUMENT);
  constructor() {
    effect(onCleanup => {
      if (!this.panel()) return;
      const body = this.document.body;
      const view = this.document.defaultView;
      const scrollY = view?.scrollY ?? 0;
      const previous = { overflow: body.style.overflow, position: body.style.position, top: body.style.top, width: body.style.width };
      Object.assign(body.style, { overflow: 'hidden', position: 'fixed', top: `-${scrollY}px`, width: '100%' });
      onCleanup(() => { Object.assign(body.style, previous); view?.scrollTo(0, scrollY); });
    });
  }

  // Formulario Ajuste / Movimiento
  ajuste = {
    productoId: null as number | null,
    sucursalId: null as number | null,
    tipo: 'ENTRADA',
    cantidad: 1,
    referencia: '',
    notas: ''
  };

  // Formulario Transferencia
  transferencia = {
    productoId: null as number | null,
    sucursalOrigenId: null as number | null,
    sucursalDestinoId: null as number | null,
    cantidad: 1,
    referencia: '',
    notas: ''
  };

  // ==========================================
  // COMPUTED - Métricas & Listados
  // ==========================================

  readonly todasExistencias = computed(() => this.catalogo()?.existencias ?? []);
  readonly todasSucursales = computed(() => this.catalogo()?.sucursales ?? []);
  readonly todosProductos = computed(() => this.catalogo()?.productos ?? []);
  readonly todosMovimientos = computed(() => this.catalogo()?.movimientos ?? []);

  // Métricas globales
  readonly totalSkus = computed(() => {
    const prods = new Set(this.todasExistencias().map(e => e.productoId));
    return prods.size;
  });

  readonly totalUnidadesFisicas = computed(() =>
    this.todasExistencias().reduce((acc, item) => acc + Number(item.existencia || 0), 0)
  );

  readonly totalUnidadesDisponibles = computed(() =>
    this.todasExistencias().reduce((acc, item) => acc + Number(item.disponible || 0), 0)
  );

  readonly totalUnidadesReservadas = computed(() =>
    this.todasExistencias().reduce((acc, item) => acc + Number(item.reservado || 0), 0)
  );

  readonly bajoStockCount = computed(() =>
    this.todasExistencias().filter(item => item.estado === 'BAJO').length
  );

  readonly agotadosCount = computed(() =>
    this.todasExistencias().filter(item => item.estado === 'AGOTADO').length
  );

  readonly disponiblesCount = computed(() =>
    this.todasExistencias().filter(item => item.estado === 'DISPONIBLE').length
  );

  readonly porcentajeSalud = computed(() => {
    const total = this.todasExistencias().length;
    if (total === 0) return 100;
    const saludables = this.disponiblesCount();
    return Math.round((saludables / total) * 100);
  });

  // Existencias filtradas y ordenadas
  readonly existenciasFiltradas = computed(() => {
    const termino = this.busqueda().trim().toLocaleLowerCase('es-MX');
    const sucursalId = this.sucursalFiltro();
    const estado = this.estadoFiltro();
    const orden = this.ordenFiltro();

    let items = this.todasExistencias().filter(item => {
      // Filtro sucursal
      if (sucursalId && item.sucursalId !== sucursalId) return false;

      // Filtro estado
      if (estado === 'DISPONIBLE' && item.estado !== 'DISPONIBLE') return false;
      if (estado === 'BAJO' && item.estado !== 'BAJO') return false;
      if (estado === 'AGOTADO' && item.estado !== 'AGOTADO') return false;
      if (estado === 'CON_RESERVA' && Number(item.reservado || 0) <= 0) return false;

      // Filtro búsqueda
      if (termino) {
        const match = [item.productoNombre, item.sku, item.sucursalNombre].some(val =>
          (val || '').toLocaleLowerCase('es-MX').includes(termino)
        );
        if (!match) return false;
      }

      return true;
    });

    // Ordenamiento
    return items.sort((a, b) => {
      switch (orden) {
        case 'urgente': {
          const peso = (e: ExistenciaProducto) => (e.estado === 'AGOTADO' ? 0 : e.estado === 'BAJO' ? 1 : 2);
          const diffPeso = peso(a) - peso(b);
          if (diffPeso !== 0) return diffPeso;
          return Number(a.disponible) - Number(b.disponible);
        }
        case 'disponible_desc':
          return Number(b.disponible) - Number(a.disponible);
        case 'disponible_asc':
          return Number(a.disponible) - Number(b.disponible);
        case 'nombre_asc':
          return a.productoNombre.localeCompare(b.productoNombre, 'es-MX');
        case 'nombre_desc':
          return b.productoNombre.localeCompare(a.productoNombre, 'es-MX');
        case 'sku':
          return a.sku.localeCompare(b.sku, 'es-MX');
        default:
          return 0;
      }
    });
  });

  // Movimientos filtrados
  readonly movimientosFiltrados = computed(() => {
    const termino = this.busquedaMovimientos().trim().toLocaleLowerCase('es-MX');
    const tipo = this.tipoMovimientoFiltro();
    const sucursalId = this.sucursalMovimientoFiltro();

    return this.todosMovimientos().filter(item => {
      if (sucursalId && item.sucursalId !== sucursalId) return false;
      if (tipo) {
        if (tipo === 'TRANSFERENCIA') {
          if (!item.tipo.startsWith('TRANSFERENCIA')) return false;
        } else if (item.tipo !== tipo) {
          return false;
        }
      }
      if (termino) {
        const match = [
          item.productoNombre,
          item.sucursalNombre,
          item.referencia ?? '',
          item.notas ?? '',
          item.tipo
        ].some(val => (val || '').toLocaleLowerCase('es-MX').includes(termino));
        if (!match) return false;
      }
      return true;
    });
  });

  // Movimientos para el panel lateral de existencias (recientes)
  readonly movimientosRecientes = computed(() => {
    const sucursalId = this.sucursalFiltro();
    return this.todosMovimientos()
      .filter(item => !sucursalId || item.sucursalId === sucursalId)
      .slice(0, 8);
  });

  // Productos para reposición sugerida
  readonly productosParaReposicion = computed<ItemReposicion[]>(() => {
    return this.todasExistencias()
      .filter(item => item.estado === 'AGOTADO' || item.estado === 'BAJO')
      .map(item => {
        const min = Number(item.stockMinimo || 1);
        const disp = Number(item.disponible || 0);
        const unidadesFaltantes = Math.max(0, min - disp);
        // Sugerencia: reabastecer al menos hasta un 150% del stock mínimo o un lote seguro
        const sugeridoReposicion = Math.max(1, Math.ceil(min * 1.5 - disp));
        return {
          ...item,
          unidadesFaltantes,
          sugeridoReposicion
        };
      })
      .sort((a, b) => {
        if (a.estado === 'AGOTADO' && b.estado !== 'AGOTADO') return -1;
        if (b.estado === 'AGOTADO' && a.estado !== 'AGOTADO') return 1;
        return b.unidadesFaltantes - a.unidadesFaltantes;
      });
  });

  // Existencia seleccionada en ajuste para cálculo en vivo
  readonly existenciaAjusteActual = computed(() => {
    const pId = this.ajuste.productoId;
    const sId = this.ajuste.sucursalId;
    if (!pId || !sId) return null;
    return this.todasExistencias().find(e => e.productoId === pId && e.sucursalId === sId) ?? null;
  });

  // Proyección de stock en formulario de ajuste
  readonly proyeccionAjuste = computed(() => {
    const ext = this.existenciaAjusteActual();
    if (!ext) return null;
    const anterior = Number(ext.existencia || 0);
    const reservado = Number(ext.reservado || 0);
    const cant = Number(this.ajuste.cantidad || 0);
    const tipo = this.ajuste.tipo;

    let nueva = anterior;
    if (tipo === 'ENTRADA') nueva = anterior + cant;
    else if (tipo === 'SALIDA') nueva = anterior - cant;
    else if (tipo === 'AJUSTE') nueva = cant;

    const nuevoDisponible = nueva - reservado;
    const esNegativo = nueva < 0;
    const afectaReservas = nueva < reservado;

    return {
      anterior,
      reservado,
      nueva,
      nuevoDisponible,
      esNegativo,
      afectaReservas
    };
  });

  // Existencia origen para transferencia
  readonly existenciaOrigenTransferencia = computed(() => {
    const pId = this.transferencia.productoId;
    const sId = this.transferencia.sucursalOrigenId;
    if (!pId || !sId) return null;
    return this.todasExistencias().find(e => e.productoId === pId && e.sucursalId === sId) ?? null;
  });

  // Movimientos de un producto seleccionado para modal de Kárdex
  readonly movimientosKardexProducto = computed(() => {
    const ext = this.productoKardex();
    if (!ext) return [];
    return this.todosMovimientos().filter(
      m => m.productoId === ext.productoId && m.sucursalId === ext.sucursalId
    );
  });

  // ==========================================
  // CICLO DE VIDA Y ACCIONES
  // ==========================================

  ngOnInit(): void {
    this.cargar();
  }

  cambiarTab(tab: TabInventario): void {
    this.tabActivo.set(tab);
    this.limpiarMensajes();
  }

  setModoVista(modo: ModoVisualizacion): void {
    this.modoVista.set(modo);
  }

  setFiltroEstado(estado: TipoFiltroEstado): void {
    this.estadoFiltro.set(estado);
  }

  limpiarFiltros(): void {
    this.busqueda.set('');
    this.sucursalFiltro.set(null);
    this.estadoFiltro.set('TODOS');
    this.ordenFiltro.set('urgente');
  }

  // ==========================================
  // CONTROL DE PANELES Y MODALES
  // ==========================================

  abrirPanelAjuste(existencia?: ExistenciaProducto, tipoPredeterminado: 'ENTRADA' | 'SALIDA' | 'AJUSTE' = 'ENTRADA'): void {
    this.limpiarMensajes();
    this.panel.set('ajuste');

    if (existencia) {
      this.ajuste.productoId = existencia.productoId;
      this.ajuste.sucursalId = existencia.sucursalId;
      this.ajuste.tipo = tipoPredeterminado;
      this.ajuste.cantidad = tipoPredeterminado === 'AJUSTE' ? Number(existencia.existencia) : 1;
    } else {
      const primerProd = this.todosProductos()[0]?.id ?? null;
      const primeraSuc = this.todasSucursales()[0]?.id ?? null;
      this.ajuste.productoId = primerProd;
      this.ajuste.sucursalId = this.sucursalFiltro() || primeraSuc;
      this.ajuste.tipo = tipoPredeterminado;
      this.ajuste.cantidad = 1;
    }
    this.ajuste.referencia = '';
    this.ajuste.notas = '';
  }

  abrirPanelTransferencia(existencia?: ExistenciaProducto): void {
    this.limpiarMensajes();
    this.panel.set('transferencia');

    const sucursales = this.todasSucursales();
    if (existencia) {
      this.transferencia.productoId = existencia.productoId;
      this.transferencia.sucursalOrigenId = existencia.sucursalId;
      const destino = sucursales.find(s => s.id !== existencia.sucursalId);
      this.transferencia.sucursalDestinoId = destino?.id ?? null;
    } else {
      const primerProd = this.todosProductos()[0]?.id ?? null;
      this.transferencia.productoId = primerProd;
      this.transferencia.sucursalOrigenId = sucursales[0]?.id ?? null;
      this.transferencia.sucursalDestinoId = sucursales[1]?.id ?? null;
    }
    this.transferencia.cantidad = 1;
    this.transferencia.referencia = '';
    this.transferencia.notas = '';
  }

  verKardex(existencia: ExistenciaProducto): void {
    this.productoKardex.set(existencia);
    this.panel.set('kardex');
  }

  cerrarPanel(): void {
    this.panel.set(null);
    this.productoKardex.set(null);
  }

  // Modificadores rápidos de formulario
  setTipoAjuste(tipo: string): void {
    this.ajuste.tipo = tipo;
    if (tipo === 'AJUSTE') {
      const ext = this.existenciaAjusteActual();
      if (ext) this.ajuste.cantidad = Number(ext.existencia);
    } else if (this.ajuste.cantidad <= 0) {
      this.ajuste.cantidad = 1;
    }
  }

  sumarCantidadAjuste(delta: number): void {
    const actual = Number(this.ajuste.cantidad) || 0;
    const nuevo = Math.max(0, actual + delta);
    this.ajuste.cantidad = Number(nuevo.toFixed(3));
  }

  setCantidadAjuste(valor: number): void {
    this.ajuste.cantidad = valor;
  }

  sumarCantidadTransferencia(delta: number): void {
    const actual = Number(this.transferencia.cantidad) || 0;
    const nuevo = Math.max(0.1, actual + delta);
    this.transferencia.cantidad = Number(nuevo.toFixed(3));
  }

  // ==========================================
  // OPERACIONES API
  // ==========================================

  guardarAjuste(): void {
    const cantidadInvalida = this.ajuste.tipo === 'AJUSTE' ? this.ajuste.cantidad < 0 : this.ajuste.cantidad <= 0;
    if (!this.ajuste.productoId || !this.ajuste.sucursalId || !this.ajuste.tipo || cantidadInvalida || this.guardando()) {
      return;
    }

    const proy = this.proyeccionAjuste();
    if (proy?.afectaReservas) {
      this.error.set('No puedes reducir la existencia por debajo de las unidades reservadas en citas o pedidos.');
      return;
    }

    this.guardando.set(true);
    this.limpiarMensajes();

    this.service.ajustar({
      productoId: this.ajuste.productoId!,
      sucursalId: this.ajuste.sucursalId!,
      tipo: this.ajuste.tipo,
      cantidad: this.ajuste.cantidad,
      referencia: this.ajuste.referencia.trim() || null,
      notas: this.ajuste.notas.trim() || null
    })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.cerrarPanel();
          this.cargar('Movimiento de inventario guardado correctamente.');
        },
        error: error => this.mostrarError(error, 'No fue posible registrar el movimiento.')
      });
  }

  guardarTransferencia(): void {
    if (
      !this.transferencia.productoId ||
      !this.transferencia.sucursalOrigenId ||
      !this.transferencia.sucursalDestinoId ||
      this.transferencia.sucursalOrigenId === this.transferencia.sucursalDestinoId ||
      this.transferencia.cantidad <= 0 ||
      this.guardando()
    ) {
      return;
    }

    const origen = this.existenciaOrigenTransferencia();
    if (origen && Number(this.transferencia.cantidad) > Number(origen.disponible)) {
      this.error.set(`La sucursal de origen solo tiene ${origen.disponible} unidades disponibles.`);
      return;
    }

    this.guardando.set(true);
    this.limpiarMensajes();

    this.service.transferir({
      productoId: this.transferencia.productoId!,
      sucursalOrigenId: this.transferencia.sucursalOrigenId!,
      sucursalDestinoId: this.transferencia.sucursalDestinoId!,
      cantidad: this.transferencia.cantidad,
      referencia: this.transferencia.referencia.trim() || null,
      notas: this.transferencia.notas.trim() || null
    })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.cerrarPanel();
          this.cargar('Transferencia entre sucursales completada con éxito.');
        },
        error: error => this.mostrarError(error, 'No fue posible realizar la transferencia.')
      });
  }

  reabastecerSugerido(item: ItemReposicion): void {
    this.abrirPanelAjuste(item, 'ENTRADA');
    this.ajuste.cantidad = item.sugeridoReposicion;
    this.ajuste.referencia = 'Reabastecimiento sugerido';
  }

  // ==========================================
  // HELPERS VISUALES Y FORMATO
  // ==========================================

  etiquetaMovimiento(tipo: string): string {
    const mapa: Record<string, string> = {
      ENTRADA: 'Entrada de mercancía',
      SALIDA: 'Salida / Merma',
      AJUSTE: 'Conteo físico',
      TRANSFERENCIA_ENTRADA: 'Transferencia recibida',
      TRANSFERENCIA_SALIDA: 'Transferencia despachada'
    };
    return mapa[tipo] ?? tipo;
  }

  esIncremento(movimiento: MovimientoInventario): boolean {
    return Number(movimiento.cantidad) >= 0;
  }

  calcularPorcentajeProgreso(item: ExistenciaProducto): number {
    const disp = Number(item.disponible || 0);
    const min = Number(item.stockMinimo || 1);
    if (disp <= 0) return 0;
    // Escala donde 100% es cuando el disponible duplica el mínimo
    const ratio = (disp / (min * 2)) * 100;
    return Math.min(100, Math.max(5, Math.round(ratio)));
  }

  // ==========================================
  // EXPORTACIONES A CSV (Auditoría & Pro)
  // ==========================================

  exportarExistenciasCsv(): void {
    const items = this.existenciasFiltradas();
    if (!items.length) return;

    const fecha = new Date().toISOString().split('T')[0];
    const headers = ['Producto', 'SKU', 'Sucursal', 'Físico', 'Reservado', 'Disponible', 'Stock Mínimo', 'Estado', 'Última Actualización'];

    const rows = items.map(item => [
      `"${(item.productoNombre || '').replace(/"/g, '""')}"`,
      `"${(item.sku || '').replace(/"/g, '""')}"`,
      `"${(item.sucursalNombre || '').replace(/"/g, '""')}"`,
      item.existencia,
      item.reservado,
      item.disponible,
      item.stockMinimo,
      item.estado,
      item.actualizadoEn ? new Date(item.actualizadoEn).toLocaleString('es-MX') : 'N/A'
    ]);

    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
    this.descargarArchivoCsv(csvContent, `inventario-existencias-${fecha}.csv`);
  }

  exportarMovimientosCsv(): void {
    const items = this.movimientosFiltrados();
    if (!items.length) return;

    const fecha = new Date().toISOString().split('T')[0];
    const headers = ['Fecha', 'Producto', 'Sucursal', 'Tipo Movimiento', 'Cantidad', 'Existencia Anterior', 'Existencia Nueva', 'Referencia', 'Notas'];

    const rows = items.map(item => [
      `"${new Date(item.creadoEn).toLocaleString('es-MX')}"`,
      `"${(item.productoNombre || '').replace(/"/g, '""')}"`,
      `"${(item.sucursalNombre || '').replace(/"/g, '""')}"`,
      `"${this.etiquetaMovimiento(item.tipo)}"`,
      item.cantidad,
      item.existenciaAnterior,
      item.existenciaNueva,
      `"${(item.referencia || '').replace(/"/g, '""')}"`,
      `"${(item.notas || '').replace(/"/g, '""')}"`
    ]);

    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
    this.descargarArchivoCsv(csvContent, `kardex-movimientos-${fecha}.csv`);
  }

  exportarReposicionCsv(): void {
    const items = this.productosParaReposicion();
    if (!items.length) return;

    const fecha = new Date().toISOString().split('T')[0];
    const headers = ['Producto', 'SKU', 'Sucursal', 'Disponible', 'Stock Mínimo', 'Déficit / Faltantes', 'Reposición Sugerida', 'Estado'];

    const rows = items.map(item => [
      `"${(item.productoNombre || '').replace(/"/g, '""')}"`,
      `"${(item.sku || '').replace(/"/g, '""')}"`,
      `"${(item.sucursalNombre || '').replace(/"/g, '""')}"`,
      item.disponible,
      item.stockMinimo,
      item.unidadesFaltantes,
      item.sugeridoReposicion,
      item.estado
    ]);

    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
    this.descargarArchivoCsv(csvContent, `orden-reposicion-${fecha}.csv`);
  }

  private descargarArchivoCsv(contenido: string, nombreArchivo: string): void {
    const blob = new Blob([contenido], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.setAttribute('href', url);
    enlace.setAttribute('download', nombreArchivo);
    document.body.appendChild(enlace);
    enlace.click();
    document.body.removeChild(enlace);
    URL.revokeObjectURL(url);
  }

  public cargar(mensaje?: string): void {
    this.cargando.set(true);
    this.service.obtenerCatalogo()
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: catalogo => {
          this.catalogo.set(catalogo);
          if (mensaje) this.mensaje.set(mensaje);
        },
        error: error => this.mostrarError(error, 'No fue posible cargar el inventario.')
      });
  }

  private mostrarError(error: any, predeterminado: string): void {
    this.error.set(error?.error?.message || error?.error?.detail || predeterminado);
  }

  private limpiarMensajes(): void {
    this.error.set(null);
    this.mensaje.set(null);
  }
}
