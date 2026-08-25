import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, NgZone, OnInit, ViewEncapsulation, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Router } from '@angular/router';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { PerfilUsuarioLocal, UserProfileService } from '../../core/profile/user-profile.service';
import { SucursalCaja } from '../../core/caja/caja.service';
import { CajaDashboardFacade } from './data/caja-dashboard.facade';
import { CajaPaymentsSectionComponent } from './payments/caja-payments-section.component';
import { CajaSessionSectionComponent } from './session/caja-session-section.component';
import { CajaMovementsSectionComponent } from './movements/caja-movements-section.component';
import { CajaReceiptComponent } from './receipt/caja-receipt.component';
import { construirComprobanteCaja } from './receipt/caja-receipt.builder';
import { CajaReceiptDto } from './receipt/caja-receipt.dto';
import { CajaReceiptPrintService } from './receipt/caja-receipt-print.service';
import { CajaHeaderComponent } from './header/caja-header.component';
import { CajaOverviewComponent } from './overview/caja-overview.component';
import { CajaViewNavigationComponent } from './navigation/caja-view-navigation.component';
import { CajaViewOption, VistaCaja } from './models/caja-dashboard.models';
import { CajaSessionFacade } from './session/caja-session.facade';
import { CajaPaymentsFacade } from './payments/caja-payments.facade';
import { CajaMovementsFacade } from './movements/caja-movements.facade';
import { CajaDashboardStore } from './state/caja-dashboard.store';
import {
  construirApertura,
  construirCierre,
  construirMovimiento,
  construirPago,
  crearFormularioApertura,
  crearFormularioCierre,
  crearFormularioMovimiento,
  crearFormularioPago
} from './forms/caja.forms';

@Component({
  selector: 'app-caja-dashboard',
  standalone: true,
  imports: [
    MatProgressBarModule,
    CajaHeaderComponent,
    CajaOverviewComponent,
    CajaViewNavigationComponent,
    CajaPaymentsSectionComponent,
    CajaSessionSectionComponent,
    CajaMovementsSectionComponent,
    CajaReceiptComponent
  ],
  templateUrl: './caja-dashboard.component.html',
  styleUrls: ['./caja-dashboard.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class CajaDashboardComponent implements OnInit, AfterViewInit {
  private readonly cajaFacade = inject(CajaDashboardFacade);
  private readonly sessionFacade = inject(CajaSessionFacade);
  private readonly paymentsFacade = inject(CajaPaymentsFacade);
  private readonly movementsFacade = inject(CajaMovementsFacade);
  private readonly store = inject(CajaDashboardStore);
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly ngZone = inject(NgZone);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly receiptPrintService = inject(CajaReceiptPrintService);
  private reintentoInicialProgramado = false;

  readonly loading = this.store.loading;
  readonly guardandoApertura = this.store.guardandoApertura;
  readonly guardandoCierre = this.store.guardandoCierre;
  readonly guardandoPago = this.store.guardandoPago;
  readonly guardandoMovimiento = this.store.guardandoMovimiento;
  readonly panelMovil = signal(false);
  readonly perfilConfigAbierto = signal(false);
  readonly vistaActiva = signal<VistaCaja>('cobros');
  readonly sucursales = this.store.sucursales;
  readonly sucursalActivaId = this.store.sucursalActivaId;
  readonly sesionActual = this.store.sesionActual;
  readonly citasPorCobrar = this.store.citasPorCobrar;
  readonly resumen = this.store.resumen;
  readonly pagosCitaSeleccionada = this.store.pagos;
  readonly movimientosDelTurno = this.store.movimientos;
  readonly ultimoMovimiento = this.store.ultimoMovimiento;
  readonly citaSeleccionadaId = this.store.citaSeleccionadaId;
  readonly error = this.store.error;
  readonly mensaje = this.store.mensaje;
  readonly metodosPago = this.store.metodosPago;
  readonly tiposMovimiento = this.store.tiposMovimiento;
  readonly estadosSesion = this.store.estadosSesion;
  readonly estadoSesionAbierta = this.store.estadoSesionAbierta;
  readonly estadoSesionCerrada = this.store.estadoSesionCerrada;
  readonly metodoPagoEfectivo = this.store.metodoPagoEfectivo;
  readonly comprobante = signal<CajaReceiptDto | null>(null);

  readonly perfilUsuario = computed(() => this.userProfileService.perfilActual());
  readonly perfilRegistrado = computed(() => this.userProfileService.perfilRegistrado());
  readonly nombreUsuario = computed(() =>
    this.perfilRegistrado()?.nombreCompleto?.trim()
    || this.perfilUsuario().nombre
    || this.authService.nombreUsuarioVisible()
  );
  readonly correoUsuario = computed(() => this.authService.sesionActual()?.correo ?? '');
  readonly puestoUsuario = computed(() =>
    this.perfilRegistrado()?.puesto?.trim()
    || this.perfilUsuario().puesto
    || 'Caja'
  );
  readonly fotoPerfilUsuario = computed(() => this.perfilUsuario().fotoDataUrl);
  readonly inicialesUsuario = computed(() => this.obtenerIniciales(this.nombreUsuario()));
  readonly nombreEmpresa = computed(() => this.authService.sesionActual()?.empresaNombre?.trim() || 'Empresa');
  readonly puedeIrRecepcion = computed(() => this.authService.puedeVerRecepcion());
  readonly puedeIrAdmin = computed(() => this.authService.puedeVerAdmin());
  readonly puedeCobrar = computed(() => this.authService.puedeCobrarCaja());
  readonly puedeGestionarSesion = computed(() => this.authService.puedeGestionarSesionCaja());
  readonly puedeGestionarMovimientos = computed(() => this.authService.puedeGestionarMovimientosCaja());
  readonly sucursalesPermitidas = computed(() => this.authService.sucursalesPermitidas());
  readonly cajaAbierta = this.store.cajaAbierta;
  readonly totalNotificaciones = this.store.totalNotificaciones;
  readonly notificacionesCaja = this.store.notificaciones;
  readonly sucursalOperativaId = computed(() =>
    this.sucursalActivaId()
    ?? this.sucursales()[0]?.id
    ?? this.sucursalesPermitidas()[0]
    ?? null
  );
  readonly sucursalActiva = computed(() => {
    const operativaId = this.sucursalOperativaId();
    return this.sucursales().find(sucursal => sucursal.id === operativaId) ?? this.sucursales()[0] ?? null;
  });
  readonly sucursalActivaNombre = computed(() => {
    const sucursal = this.sucursalActiva();
    if (sucursal?.nombre) {
      return sucursal.nombre;
    }
    const desdeSesion = this.sesionActual()?.sucursalNombre?.trim();
    if (desdeSesion) {
      return desdeSesion;
    }
    const desdeCita = this.citasPorCobrar()[0]?.sucursalNombre?.trim();
    if (desdeCita) {
      return desdeCita;
    }
    return this.sucursalOperativaId() ? `Sucursal ${this.sucursalOperativaId()}` : '';
  });
  readonly sucursalActivaDireccion = computed(() => this.sucursalActiva()?.direccion ?? '');
  readonly sucursalActivaResuelta = computed(() => Boolean(this.sucursalActivaNombre()));
  readonly estadoSesionEtiqueta = this.store.estadoSesionEtiqueta;
  readonly mensajePagoBloqueado = computed(() =>
    this.cajaAbierta()
      ? ''
      : `Abre caja${this.sucursalOperativaId() ? ' en la sucursal activa' : ''} para registrar cobros.`
  );
  readonly citaSeleccionada = this.store.citaSeleccionada;
  readonly citaSeleccionadaActualId = computed(() => this.citaSeleccionada()?.citaId ?? null);
  readonly vistasDisponibles = computed(() => {
    const vistas: CajaViewOption[] = [];
    if (this.puedeCobrar()) {
      vistas.push({ id: 'cobros', label: 'Cobros' });
    }
    if (this.puedeGestionarSesion()) {
      vistas.push({ id: 'sesion', label: 'Apertura y cierre' });
    }
    if (this.puedeGestionarMovimientos()) {
      vistas.push({ id: 'movimientos', label: 'Caja chica' });
    }
    return vistas;
  });
  readonly metricas = this.store.metricas;

  formularioApertura = crearFormularioApertura();
  formularioCierre = crearFormularioCierre();
  formularioPago = crearFormularioPago();
  formularioMovimiento = crearFormularioMovimiento();

  ngOnInit(): void {
    this.authService.sincronizarSesionPersistida();
    this.userProfileService.cargar();
    this.actualizarVistaEnZona(() => {
      this.sincronizarSucursalOperativa(this.sucursalesPermitidas()[0] ?? null);
      this.vistaActiva.set(this.obtenerVistaInicial());
    });

    this.breakpointObserver
      .observe('(max-width: 991px)')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ matches }) => {
        this.actualizarVistaEnZona(() => {
          this.panelMovil.set(matches);
        });
      });

    this.cargarCatalogoYTablero();
  }

  ngAfterViewInit(): void {
    this.programarReintentoInicial();
  }

  private actualizarVistaEnZona(actualizacion: () => void) {
    this.ngZone.run(() => {
      actualizacion();
      this.changeDetectorRef.detectChanges();
    });
  }

  private programarReintentoInicial() {
    if (this.reintentoInicialProgramado) {
      return;
    }

    this.reintentoInicialProgramado = true;
    setTimeout(() => {
      if (this.tableroInicialIncompleto()) {
        this.cargarCatalogoYTablero();
      }
    }, 180);
  }

  private tableroInicialIncompleto(): boolean {
    return !this.loading() && (
      !this.sucursalOperativaId()
      || !this.sucursalActivaResuelta()
      || !this.resumen()
      || !this.sesionActual() && !this.citasPorCobrar().length && !this.error()
    );
  }

  cargarCatalogoYTablero(sucursalIdPreferida?: number | null) {
    this.store.setLoading(true);
    this.store.setError('');

    const sucursalBase = sucursalIdPreferida ?? this.obtenerSucursalOperativaId();

    this.cajaFacade.cargarConCatalogo(sucursalBase)
      .pipe(finalize(() => this.store.setLoading(false)))
      .subscribe({
        next: ({ catalogo, tablero: { sesion, citas, resumen, movimientos } }) => {
          this.actualizarVistaEnZona(() => {
            this.store.applyCatalog(catalogo);
            this.sincronizarCatalogoOperativo(catalogo.sucursalActivaId);
            this.store.applyDashboard({ sesion, citas, resumen, movimientos });
            this.sincronizarCitaSeleccionada();
            this.formularioCierre.montoContado = Number(resumen.saldoEsperadoCaja ?? sesion?.montoEsperado ?? 0);
          });
        },
        error: error => {
          this.actualizarVistaEnZona(() => {
            this.store.setError(this.extraerMensaje(error, 'No pude cargar las sucursales para Caja.'));
          });
        }
      });
  }

  recargarTablero(sucursalIdForzado?: number | null) {
    const sucursalId = sucursalIdForzado ?? this.sucursalOperativaId();
    this.store.setLoading(true);
    this.store.setError('');

    this.cajaFacade.cargarTablero(sucursalId)
      .pipe(finalize(() => this.store.setLoading(false)))
      .subscribe({
        next: ({ sesion, citas, resumen, movimientos }) => {
          this.actualizarVistaEnZona(() => {
            this.store.applyDashboard({ sesion, citas, resumen, movimientos });
            this.sincronizarCitaSeleccionada();
            this.formularioCierre.montoContado = Number(resumen.saldoEsperadoCaja ?? sesion?.montoEsperado ?? 0);
          });
        },
        error: error => {
          this.actualizarVistaEnZona(() => {
            this.store.setError(this.extraerMensaje(error, 'No pude actualizar el tablero de Caja.'));
          });
        }
      });
  }

  seleccionarSucursal(sucursalId: number | null) {
    this.sincronizarSucursalOperativa(sucursalId);
    this.store.clearAppointmentContext();
    this.cargarCatalogoYTablero(sucursalId);
  }

  abrirCaja() {
    const sucursalId = this.obtenerSucursalOperativaId();
    if (!sucursalId) {
      this.store.setError('Selecciona una sucursal antes de abrir la caja.');
      return;
    }

    this.store.setSavingOpen(true);
    this.store.setError('');
    this.store.setMessage('');

    this.sessionFacade.open(construirApertura(sucursalId, this.formularioApertura))
      .pipe(finalize(() => this.store.setSavingOpen(false)))
      .subscribe({
        next: sesion => {
          this.store.setSession(sesion);
          this.formularioApertura = crearFormularioApertura();
          this.store.setMessage('Caja abierta correctamente.');
          this.recargarTablero();
        },
        error: error => {
          this.store.setError(this.extraerMensaje(error, 'No pude abrir la caja.'));
        }
      });
  }

  cerrarCaja() {
    const sesion = this.sesionActual();
    if (!sesion) {
      this.store.setError('No hay una caja abierta para cerrar.');
      return;
    }

    this.store.setSavingClose(true);
    this.store.setError('');
    this.store.setMessage('');

    this.sessionFacade.close(sesion.id, construirCierre(this.formularioCierre))
      .pipe(finalize(() => this.store.setSavingClose(false)))
      .subscribe({
        next: respuesta => {
          this.store.setSession(respuesta);
          this.store.setMessage('Caja cerrada correctamente.');
          this.recargarTablero();
        },
        error: error => {
          this.store.setError(this.extraerMensaje(error, 'No pude cerrar la caja.'));
        }
      });
  }

  seleccionarCita(citaId: number) {
    this.store.selectAppointment(citaId);
    const cita = this.citasPorCobrar().find(item => item.citaId === citaId);
    if (cita) {
      this.formularioPago.monto = Number(cita.pendiente);
      this.formularioPago.montoRecibido = Number(cita.pendiente);
    }

    this.paymentsFacade.list(citaId).subscribe({
      next: pagos => {
        this.actualizarVistaEnZona(() => {
          this.store.setPayments(pagos);
        });
      },
      error: error => {
        this.actualizarVistaEnZona(() => {
          this.store.setError(this.extraerMensaje(error, 'No pude cargar el historial de pagos de la cita.'));
        });
      }
    });
  }

  registrarPago() {
    const cita = this.citaSeleccionada();
    if (!cita) {
      this.store.setError('Selecciona una cita por cobrar antes de registrar un pago.');
      return;
    }

    this.store.setSavingPayment(true);
    this.store.setError('');
    this.store.setMessage('');

    this.paymentsFacade.register(cita.citaId, construirPago(this.formularioPago))
      .pipe(finalize(() => this.store.setSavingPayment(false)))
      .subscribe({
        next: pago => {
          this.store.setMessage('Pago registrado correctamente.');
          this.formularioPago.montoRecibido = this.formularioPago.monto;
          this.formularioPago.referencia = '';
          this.formularioPago.observaciones = '';
          this.store.prependPayment(pago);
          this.recargarTablero();
        },
        error: error => {
          this.store.setError(this.extraerMensaje(error, 'No pude registrar el pago.'));
        }
      });
  }

  registrarMovimiento() {
    const sucursalId = this.obtenerSucursalOperativaId();
    if (!sucursalId) {
      this.store.setError('Selecciona una sucursal antes de registrar movimientos de caja.');
      return;
    }

    this.store.setSavingMovement(true);
    this.store.setError('');
    this.store.setMessage('');

    this.movementsFacade.register(construirMovimiento(sucursalId, this.formularioMovimiento))
      .pipe(finalize(() => this.store.setSavingMovement(false)))
      .subscribe({
        next: movimiento => {
          this.formularioMovimiento = crearFormularioMovimiento(
            this.tiposMovimiento()[0]?.codigo,
            this.metodosPago()[0]?.codigo
          );
          this.store.setMessage('Movimiento registrado correctamente.');
          this.recargarTablero();
        },
        error: error => {
          this.store.setError(this.extraerMensaje(error, 'No pude registrar el movimiento de caja.'));
        }
      });
  }

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  abrirConfiguracionPerfil() {
    this.perfilConfigAbierto.set(true);
  }

  cerrarConfiguracionPerfil() {
    this.perfilConfigAbierto.set(false);
  }

  guardarConfiguracionPerfil(perfil: PerfilUsuarioLocal) {
    this.userProfileService.guardar(perfil);
    this.perfilConfigAbierto.set(false);
    this.store.setMessage('Perfil actualizado.');
  }

  mostrarErrorPerfil(mensaje: string) {
    this.store.setError(mensaje);
  }

  irARecepcion() {
    this.ngZone.run(() => {
      void this.router.navigateByUrl('/recepcion');
    });
  }

  irAAdmin() {
    this.ngZone.run(() => {
      void this.router.navigateByUrl('/admin');
    });
  }

  private obtenerIniciales(nombre: string): string {
    return nombre
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(parte => parte[0]?.toUpperCase() ?? '')
      .join('') || 'US';
  }

  seleccionarVista(vista: VistaCaja) {
    this.vistaActiva.set(vista);
  }

  async imprimirComprobante() {
    const cita = this.citaSeleccionada();
    if (!cita) {
      this.store.setError('Selecciona una cita para imprimir el comprobante.');
      return;
    }

    const pagos = this.pagosCitaSeleccionada();
    if (!pagos.length) {
      this.store.setError('Todavía no hay pagos registrados para imprimir un comprobante.');
      return;
    }

    this.comprobante.set(construirComprobanteCaja(this.nombreEmpresa(), cita, pagos, this.metodosPago()));
    try {
      await this.receiptPrintService.printAfterRender();
    } catch (error) {
      this.store.setError(this.extraerMensaje(error, 'No pude preparar el comprobante para impresión.'));
    }
  }

  private obtenerSucursalOperativaId(): number | null {
    return this.sucursalActivaId()
      ?? this.sucursales()[0]?.id
      ?? this.sucursalesPermitidas()[0]
      ?? null;
  }

  private resolverSucursalOperativaInicial(sucursalesVisibles: SucursalCaja[]): number | null {
    return this.sucursalActivaId()
      ?? sucursalesVisibles[0]?.id
      ?? this.sucursalesPermitidas()[0]
      ?? null;
  }

  private sincronizarCatalogoOperativo(sucursalCatalogoId: number | null) {
    if (!this.metodosPago().some(metodo => metodo.codigo === this.formularioPago.metodoPago)) {
      this.formularioPago.metodoPago = this.metodosPago()[0]?.codigo ?? '';
    }
    if (!this.metodosPago().some(metodo => metodo.codigo === this.formularioMovimiento.metodoPago)) {
      this.formularioMovimiento.metodoPago = this.metodosPago()[0]?.codigo ?? '';
    }
    if (!this.tiposMovimiento().some(tipo => tipo.codigo === this.formularioMovimiento.tipoMovimiento)) {
      this.formularioMovimiento.tipoMovimiento = this.tiposMovimiento()[0]?.codigo ?? '';
    }
    const sucursalOperativa = sucursalCatalogoId
      ?? this.sucursalesPermitidas()[0]
      ?? this.sucursales()[0]?.id
      ?? null;
    this.sincronizarSucursalOperativa(sucursalOperativa);
  }

  private obtenerVistaInicial(): VistaCaja {
    if (this.puedeCobrar()) {
      return 'cobros';
    }
    if (this.puedeGestionarSesion()) {
      return 'sesion';
    }
    return 'movimientos';
  }

  private sincronizarSucursalOperativa(sucursalId: number | null) {
    this.store.setActiveBranch(sucursalId);
  }

  private sincronizarCitaSeleccionada() {
    const citas = this.citasPorCobrar();
    if (!citas.length) {
      this.store.clearAppointmentContext();
      return;
    }

    const citaSeleccionadaId = this.citaSeleccionadaId();
    const citaSeleccionadaExiste = citas.some(cita => cita.citaId === citaSeleccionadaId);
    const citaId = citaSeleccionadaExiste ? citaSeleccionadaId! : citas[0].citaId;
    if (this.citaSeleccionadaId() !== citaId) {
      this.seleccionarCita(citaId);
      return;
    }

    const cita = citas.find(item => item.citaId === citaId);
    if (cita && this.formularioPago.monto <= 0) {
      this.formularioPago.monto = Number(cita.pendiente);
      this.formularioPago.montoRecibido = Number(cita.pendiente);
    }
  }
  private extraerMensaje(error: unknown, fallback: string): string {
    const httpError = error as { error?: { message?: string }; message?: string };
    return httpError?.error?.message || httpError?.message || fallback;
  }

}
