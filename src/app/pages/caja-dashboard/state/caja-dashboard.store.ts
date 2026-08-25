import { Injectable, computed, signal } from '@angular/core';
import { CajaSesion, CatalogoCaja, CitaPorCobrar, MovimientoCaja, PagoCita, ResumenCaja, SucursalCaja } from '../../../core/caja/caja.service';
import { CajaMetricViewModel, CajaNotificationViewModel } from '../models/caja-dashboard.models';

export interface CajaDashboardSnapshot {
  sesion: CajaSesion | null;
  citas: CitaPorCobrar[];
  resumen: ResumenCaja;
  movimientos: MovimientoCaja[];
}

@Injectable({ providedIn: 'root' })
export class CajaDashboardStore {
  private readonly _loading = signal(false);
  private readonly _guardandoApertura = signal(false);
  private readonly _guardandoCierre = signal(false);
  private readonly _guardandoPago = signal(false);
  private readonly _guardandoMovimiento = signal(false);
  private readonly _sucursales = signal<SucursalCaja[]>([]);
  private readonly _sucursalActivaId = signal<number | null>(null);
  private readonly _sesionActual = signal<CajaSesion | null>(null);
  private readonly _citasPorCobrar = signal<CitaPorCobrar[]>([]);
  private readonly _resumen = signal<ResumenCaja | null>(null);
  private readonly _pagos = signal<PagoCita[]>([]);
  private readonly _movimientos = signal<MovimientoCaja[]>([]);
  private readonly _citaSeleccionadaId = signal<number | null>(null);
  private readonly _error = signal('');
  private readonly _mensaje = signal('');
  private readonly _metodosPago = signal<CatalogoCaja['metodosPago']>([]);
  private readonly _tiposMovimiento = signal<CatalogoCaja['tiposMovimiento']>([]);
  private readonly _estadosSesion = signal<CatalogoCaja['estadosSesion']>([]);
  private readonly _estadoSesionAbierta = signal('');
  private readonly _estadoSesionCerrada = signal('');
  private readonly _metodoPagoEfectivo = signal('');

  readonly loading = this._loading.asReadonly();
  readonly guardandoApertura = this._guardandoApertura.asReadonly();
  readonly guardandoCierre = this._guardandoCierre.asReadonly();
  readonly guardandoPago = this._guardandoPago.asReadonly();
  readonly guardandoMovimiento = this._guardandoMovimiento.asReadonly();
  readonly sucursales = this._sucursales.asReadonly();
  readonly sucursalActivaId = this._sucursalActivaId.asReadonly();
  readonly sesionActual = this._sesionActual.asReadonly();
  readonly citasPorCobrar = this._citasPorCobrar.asReadonly();
  readonly resumen = this._resumen.asReadonly();
  readonly pagos = this._pagos.asReadonly();
  readonly movimientos = this._movimientos.asReadonly();
  readonly citaSeleccionadaId = this._citaSeleccionadaId.asReadonly();
  readonly error = this._error.asReadonly();
  readonly mensaje = this._mensaje.asReadonly();
  readonly metodosPago = this._metodosPago.asReadonly();
  readonly tiposMovimiento = this._tiposMovimiento.asReadonly();
  readonly estadosSesion = this._estadosSesion.asReadonly();
  readonly estadoSesionAbierta = this._estadoSesionAbierta.asReadonly();
  readonly estadoSesionCerrada = this._estadoSesionCerrada.asReadonly();
  readonly metodoPagoEfectivo = this._metodoPagoEfectivo.asReadonly();

  readonly cajaAbierta = computed(() => Boolean(this._estadoSesionAbierta() && this._sesionActual()?.estado === this._estadoSesionAbierta()));
  readonly ultimoMovimiento = computed(() => this._movimientos()[0] ?? null);
  readonly citaSeleccionada = computed(() => this._citasPorCobrar().find(cita => cita.citaId === this._citaSeleccionadaId()) ?? this._citasPorCobrar()[0] ?? null);
  readonly totalPendiente = computed(() => this._citasPorCobrar().reduce((total, cita) => total + (Number(cita.pendiente) || 0), 0));
  readonly totalNotificaciones = computed(() => this._citasPorCobrar().length + (this.cajaAbierta() ? 0 : 1));
  readonly estadoSesionEtiqueta = computed(() => {
    const codigo = this.cajaAbierta() ? this._estadoSesionAbierta() : this._estadoSesionCerrada();
    return this._estadosSesion().find(estado => estado.codigo === codigo)?.etiqueta ?? codigo;
  });
  readonly notificaciones = computed<CajaNotificationViewModel[]>(() => [
    !this.cajaAbierta() ? { titulo: 'Caja cerrada', descripcion: 'Abre una sesión de caja para operar cobros.', icono: 'bi-cash-coin', total: 1 } : null,
    this._citasPorCobrar().length ? { titulo: 'Citas por cobrar', descripcion: `${this._citasPorCobrar().length} citas tienen saldo pendiente.`, icono: 'bi-receipt', total: this._citasPorCobrar().length } : null
  ].filter((item): item is CajaNotificationViewModel => item !== null));
  readonly metricas = computed<CajaMetricViewModel[]>(() => [
    { label: 'Pendiente por cobrar', value: this.totalPendiente(), accent: 'primary', format: 'money' },
    { label: 'Cobrado en turno', value: Number(this._resumen()?.totalCobrado ?? 0), accent: 'neutral', format: 'money' },
    { label: 'Efectivo esperado', value: Number(this._resumen()?.saldoEsperadoCaja ?? 0), accent: 'neutral', format: 'money' },
    { label: 'Citas por cobrar', value: this._citasPorCobrar().length, accent: 'soft', format: 'count' }
  ]);

  setLoading(value: boolean): void { this._loading.set(value); }
  setSavingOpen(value: boolean): void { this._guardandoApertura.set(value); }
  setSavingClose(value: boolean): void { this._guardandoCierre.set(value); }
  setSavingPayment(value: boolean): void { this._guardandoPago.set(value); }
  setSavingMovement(value: boolean): void { this._guardandoMovimiento.set(value); }
  setError(value: string): void { this._error.set(value); }
  setMessage(value: string): void { this._mensaje.set(value); }
  setActiveBranch(id: number | null): void { this._sucursalActivaId.set(id); }
  setSession(session: CajaSesion | null): void { this._sesionActual.set(session); }
  setPayments(payments: PagoCita[]): void { this._pagos.set(payments); }
  prependPayment(payment: PagoCita): void { this._pagos.update(payments => [payment, ...payments]); }
  selectAppointment(id: number | null): void { this._citaSeleccionadaId.set(id); }

  applyCatalog(catalog: CatalogoCaja): void {
    this._sucursales.set(catalog.sucursales ?? []);
    this._metodosPago.set(catalog.metodosPago ?? []);
    this._tiposMovimiento.set(catalog.tiposMovimiento ?? []);
    this._estadosSesion.set(catalog.estadosSesion ?? []);
    this._estadoSesionAbierta.set(catalog.estadoSesionAbierta ?? '');
    this._estadoSesionCerrada.set(catalog.estadoSesionCerrada ?? '');
    this._metodoPagoEfectivo.set(catalog.metodoPagoEfectivo ?? '');
  }

  applyDashboard(snapshot: CajaDashboardSnapshot): void {
    this._sesionActual.set(snapshot.sesion);
    this._citasPorCobrar.set(snapshot.citas);
    this._resumen.set(snapshot.resumen);
    this._movimientos.set(snapshot.movimientos);
  }

  clearAppointmentContext(): void {
    this._citaSeleccionadaId.set(null);
    this._pagos.set([]);
  }
}
