import { CommonModule, DatePipe } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, NgZone, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatToolbarModule } from '@angular/material/toolbar';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { PerfilUsuarioLocal, UserProfileService } from '../../core/profile/user-profile.service';
import { UserProfileDialogComponent } from '../../shared/profile/user-profile-dialog.component';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
import {
  CajaSesion,
  CatalogoCaja,
  CitaPorCobrar,
  MovimientoCaja,
  PagoCita,
  ResumenCaja,
  SucursalCaja
} from '../../core/caja/caja.service';
import { CajaDashboardFacade } from './data/caja-dashboard.facade';
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

type VistaCaja = 'cobros' | 'sesion' | 'movimientos';

@Component({
  selector: 'app-caja-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    MoneyDisplayPipe,
    MatBadgeModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatProgressBarModule,
    MatSelectModule,
    MatToolbarModule,
    UserProfileDialogComponent
  ],
  templateUrl: './caja-dashboard.component.html',
  styleUrls: ['./caja-dashboard.component.css']
})
export class CajaDashboardComponent implements OnInit, AfterViewInit {
  private readonly cajaFacade = inject(CajaDashboardFacade);
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly ngZone = inject(NgZone);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private reintentoInicialProgramado = false;

  readonly loading = signal(false);
  readonly guardandoApertura = signal(false);
  readonly guardandoCierre = signal(false);
  readonly guardandoPago = signal(false);
  readonly guardandoMovimiento = signal(false);
  readonly panelMovil = signal(false);
  readonly perfilConfigAbierto = signal(false);
  readonly vistaActiva = signal<VistaCaja>('cobros');
  readonly sucursales = signal<SucursalCaja[]>([]);
  readonly sucursalActivaId = signal<number | null>(null);
  readonly sesionActual = signal<CajaSesion | null>(null);
  readonly citasPorCobrar = signal<CitaPorCobrar[]>([]);
  readonly resumen = signal<ResumenCaja | null>(null);
  readonly pagosCitaSeleccionada = signal<PagoCita[]>([]);
  readonly movimientosDelTurno = signal<MovimientoCaja[]>([]);
  readonly ultimoMovimiento = signal<MovimientoCaja | null>(null);
  readonly citaSeleccionadaId = signal<number | null>(null);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly metodosPago = signal<CatalogoCaja['metodosPago']>([]);
  readonly tiposMovimiento = signal<CatalogoCaja['tiposMovimiento']>([]);
  readonly estadoSesionAbierta = signal('');
  readonly metodoPagoEfectivo = signal('');

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
  readonly cajaAbierta = computed(() => Boolean(
    this.estadoSesionAbierta() && this.sesionActual()?.estado === this.estadoSesionAbierta()
  ));
  readonly totalNotificaciones = computed(() => this.citasPorCobrar().length + (this.cajaAbierta() ? 0 : 1));
  readonly notificacionesCaja = computed(() => [
    !this.cajaAbierta() ? { titulo: 'Caja cerrada', descripcion: 'Abre una sesión de caja para operar cobros.', icono: 'bi-cash-coin', total: 1 } : null,
    this.citasPorCobrar().length ? { titulo: 'Citas por cobrar', descripcion: `${this.citasPorCobrar().length} citas tienen saldo pendiente.`, icono: 'bi-receipt', total: this.citasPorCobrar().length } : null
  ].filter((item): item is { titulo: string; descripcion: string; icono: string; total: number } => !!item));
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
  readonly mensajePagoBloqueado = computed(() =>
    this.cajaAbierta()
      ? ''
      : `Abre caja${this.sucursalOperativaId() ? ' en la sucursal activa' : ''} para registrar cobros.`
  );
  readonly citaSeleccionada = computed(() =>
    this.citasPorCobrar().find(cita => cita.citaId === this.citaSeleccionadaId()) ?? this.citasPorCobrar()[0] ?? null
  );
  readonly citaSeleccionadaActualId = computed(() => this.citaSeleccionada()?.citaId ?? null);
  readonly vistasDisponibles = computed(() => {
    const vistas: Array<{ id: VistaCaja; label: string }> = [];
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
  readonly totalPendiente = computed(() =>
    this.citasPorCobrar().reduce((total, cita) => total + (Number(cita.pendiente) || 0), 0)
  );
  readonly metricas = computed(() => [
    { label: 'Pendiente por cobrar', value: this.totalPendiente(), accent: 'primary' },
    { label: 'Cobrado en turno', value: Number(this.resumen()?.totalCobrado ?? 0), accent: 'neutral' },
    { label: 'Efectivo esperado', value: Number(this.resumen()?.saldoEsperadoCaja ?? 0), accent: 'neutral' },
    { label: 'Citas por cobrar', value: this.citasPorCobrar().length, accent: 'soft' }
  ]);

  formularioApertura = crearFormularioApertura();
  formularioCierre = crearFormularioCierre();
  formularioPago = crearFormularioPago();
  formularioMovimiento = crearFormularioMovimiento();

  sucursalSeleccionadaModel: number | null = null;

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
    this.loading.set(true);
    this.error.set('');

    const sucursalBase = sucursalIdPreferida ?? this.obtenerSucursalOperativaId();

    this.cajaFacade.cargarConCatalogo(sucursalBase)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ catalogo, tablero: { sesion, citas, resumen, movimientos } }) => {
          this.actualizarVistaEnZona(() => {
            this.aplicarCatalogo(catalogo);
            this.sesionActual.set(sesion);
            this.citasPorCobrar.set(citas);
            this.resumen.set(resumen);
            this.movimientosDelTurno.set(movimientos);
            this.ultimoMovimiento.set(movimientos[0] ?? null);
            this.sincronizarCitaSeleccionada();
            this.formularioCierre.montoContado = Number(resumen.saldoEsperadoCaja ?? sesion?.montoEsperado ?? 0);
          });
        },
        error: error => {
          this.actualizarVistaEnZona(() => {
            this.error.set(this.extraerMensaje(error, 'No pude cargar las sucursales para Caja.'));
          });
        }
      });
  }

  recargarTablero(sucursalIdForzado?: number | null) {
    const sucursalId = sucursalIdForzado ?? this.sucursalOperativaId();
    this.loading.set(true);
    this.error.set('');

    this.cajaFacade.cargarTablero(sucursalId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ sesion, citas, resumen, movimientos }) => {
          this.actualizarVistaEnZona(() => {
            this.sesionActual.set(sesion);
            this.citasPorCobrar.set(citas);
            this.resumen.set(resumen);
            this.movimientosDelTurno.set(movimientos);
            this.ultimoMovimiento.set(movimientos[0] ?? null);
            this.sincronizarCitaSeleccionada();
            this.formularioCierre.montoContado = Number(resumen.saldoEsperadoCaja ?? sesion?.montoEsperado ?? 0);
          });
        },
        error: error => {
          this.actualizarVistaEnZona(() => {
            this.error.set(this.extraerMensaje(error, 'No pude actualizar el tablero de Caja.'));
          });
        }
      });
  }

  seleccionarSucursal(sucursalId: number | null) {
    this.sincronizarSucursalOperativa(sucursalId);
    this.citaSeleccionadaId.set(null);
    this.pagosCitaSeleccionada.set([]);
    this.cargarCatalogoYTablero(sucursalId);
  }

  abrirCaja() {
    const sucursalId = this.obtenerSucursalOperativaId();
    if (!sucursalId) {
      this.error.set('Selecciona una sucursal antes de abrir la caja.');
      return;
    }

    this.guardandoApertura.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.cajaFacade.abrirCaja(construirApertura(sucursalId, this.formularioApertura))
      .pipe(finalize(() => this.guardandoApertura.set(false)))
      .subscribe({
        next: sesion => {
          this.sesionActual.set(sesion);
          this.formularioApertura = crearFormularioApertura();
          this.mensaje.set('Caja abierta correctamente.');
          this.recargarTablero();
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude abrir la caja.'));
        }
      });
  }

  cerrarCaja() {
    const sesion = this.sesionActual();
    if (!sesion) {
      this.error.set('No hay una caja abierta para cerrar.');
      return;
    }

    this.guardandoCierre.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.cajaFacade.cerrarCaja(sesion.id, construirCierre(this.formularioCierre))
      .pipe(finalize(() => this.guardandoCierre.set(false)))
      .subscribe({
        next: respuesta => {
          this.sesionActual.set(respuesta);
          this.mensaje.set('Caja cerrada correctamente.');
          this.recargarTablero();
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude cerrar la caja.'));
        }
      });
  }

  seleccionarCita(citaId: number) {
    this.citaSeleccionadaId.set(citaId);
    const cita = this.citasPorCobrar().find(item => item.citaId === citaId);
    if (cita) {
      this.formularioPago.monto = Number(cita.pendiente);
      this.formularioPago.montoRecibido = Number(cita.pendiente);
    }

    this.cajaFacade.listarPagos(citaId).subscribe({
      next: pagos => {
        this.actualizarVistaEnZona(() => {
          this.pagosCitaSeleccionada.set(pagos);
        });
      },
      error: error => {
        this.actualizarVistaEnZona(() => {
          this.error.set(this.extraerMensaje(error, 'No pude cargar el historial de pagos de la cita.'));
        });
      }
    });
  }

  registrarPago() {
    const cita = this.citaSeleccionada();
    if (!cita) {
      this.error.set('Selecciona una cita por cobrar antes de registrar un pago.');
      return;
    }

    this.guardandoPago.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.cajaFacade.registrarPago(cita.citaId, construirPago(this.formularioPago))
      .pipe(finalize(() => this.guardandoPago.set(false)))
      .subscribe({
        next: pago => {
          this.mensaje.set('Pago registrado correctamente.');
          this.formularioPago.montoRecibido = this.formularioPago.monto;
          this.formularioPago.referencia = '';
          this.formularioPago.observaciones = '';
          this.pagosCitaSeleccionada.set([pago, ...this.pagosCitaSeleccionada()]);
          this.recargarTablero();
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude registrar el pago.'));
        }
      });
  }

  registrarMovimiento() {
    const sucursalId = this.obtenerSucursalOperativaId();
    if (!sucursalId) {
      this.error.set('Selecciona una sucursal antes de registrar movimientos de caja.');
      return;
    }

    this.guardandoMovimiento.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.cajaFacade.registrarMovimiento(construirMovimiento(sucursalId, this.formularioMovimiento))
      .pipe(finalize(() => this.guardandoMovimiento.set(false)))
      .subscribe({
        next: movimiento => {
          this.formularioMovimiento = crearFormularioMovimiento(
            this.tiposMovimiento()[0]?.codigo,
            this.metodosPago()[0]?.codigo
          );
          this.mensaje.set('Movimiento registrado correctamente.');
          this.recargarTablero();
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude registrar el movimiento de caja.'));
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
    this.mensaje.set('Perfil actualizado.');
  }

  mostrarErrorPerfil(mensaje: string) {
    this.error.set(mensaje);
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

  imprimirComprobante() {
    const cita = this.citaSeleccionada();
    if (!cita) {
      this.error.set('Selecciona una cita para imprimir el comprobante.');
      return;
    }

    const pagos = this.pagosCitaSeleccionada();
    if (!pagos.length) {
      this.error.set('Todavía no hay pagos registrados para imprimir un comprobante.');
      return;
    }

    const totalPagado = pagos.reduce((total, pago) => total + Number(pago.monto || 0), 0);
    const pendiente = Math.max(Number(cita.total || 0) - totalPagado, 0);
    const ventana = window.open('', '_blank', 'width=820,height=900');
    if (!ventana) {
      this.error.set('No pude abrir la ventana de impresión. Revisa si el navegador está bloqueando ventanas emergentes.');
      return;
    }

    const filasPagos = pagos
      .map(pago => `
        <tr>
          <td>${this.formatearFechaHora(pago.registradoEn)}</td>
          <td>${pago.metodoPago}</td>
          <td>${pago.referencia ?? '-'}</td>
          <td style="text-align:right;">${this.formatearMoneda(Number(pago.monto || 0))}</td>
        </tr>
      `)
      .join('');

    ventana.document.write(`
      <html lang="es">
        <head>
          <title>Comprobante de cobro</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 28px; color: #1f2937; }
            h1, h2, p { margin: 0; }
            .header { margin-bottom: 24px; }
            .header small { color: #6b7280; display: block; margin-top: 6px; }
            .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 20px 0; }
            .card { border: 1px solid #e5e7eb; border-radius: 12px; padding: 14px; }
            .label { display: block; font-size: 12px; text-transform: uppercase; color: #6b7280; margin-bottom: 6px; }
            .value { font-size: 18px; font-weight: 700; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; }
            th, td { padding: 10px 8px; border-bottom: 1px solid #e5e7eb; font-size: 14px; text-align: left; }
            th { color: #6b7280; text-transform: uppercase; font-size: 12px; }
            .totals { margin-top: 24px; width: 280px; margin-left: auto; }
            .totals div { display: flex; justify-content: space-between; margin-bottom: 8px; }
            .totals strong { font-size: 18px; }
          </style>
        </head>
        <body>
          <div class="header">
            <h1>${this.nombreEmpresa()}</h1>
            <small>Comprobante de cobro</small>
          </div>

          <div class="grid">
            <div class="card">
              <span class="label">Cliente</span>
              <div class="value">${cita.clienteNombre}</div>
            </div>
            <div class="card">
              <span class="label">Sucursal</span>
              <div class="value">${cita.sucursalNombre}</div>
            </div>
            <div class="card">
              <span class="label">Servicio</span>
              <div class="value">${cita.servicioNombre}</div>
            </div>
            <div class="card">
              <span class="label">Cita</span>
              <div class="value">${this.formatearFechaHora(cita.inicio)}</div>
            </div>
          </div>

          <h2>Pagos registrados</h2>
          <table>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Método</th>
                <th>Referencia</th>
                <th style="text-align:right;">Monto</th>
              </tr>
            </thead>
            <tbody>
              ${filasPagos}
            </tbody>
          </table>

          <div class="totals">
            <div><span>Total cita</span><span>${this.formatearMoneda(Number(cita.total || 0))}</span></div>
            <div><span>Total pagado</span><span>${this.formatearMoneda(totalPagado)}</span></div>
            <div><span>Pendiente</span><strong>${this.formatearMoneda(pendiente)}</strong></div>
          </div>
        </body>
      </html>
    `);
    ventana.document.close();
    ventana.focus();
    ventana.print();
  }

  calcularCambioPago(): number {
    if (this.formularioPago.metodoPago !== this.metodoPagoEfectivo()) {
      return 0;
    }
    const monto = Number(this.formularioPago.monto || 0);
    const recibido = Number(this.formularioPago.montoRecibido || 0);
    if (!Number.isFinite(monto) || !Number.isFinite(recibido) || recibido <= monto) {
      return 0;
    }
    return recibido - monto;
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

  private aplicarCatalogo(catalogo: CatalogoCaja) {
    this.sucursales.set(catalogo.sucursales ?? []);
    this.metodosPago.set(catalogo.metodosPago ?? []);
    this.tiposMovimiento.set(catalogo.tiposMovimiento ?? []);
    this.estadoSesionAbierta.set(catalogo.estadoSesionAbierta ?? '');
    this.metodoPagoEfectivo.set(catalogo.metodoPagoEfectivo ?? '');
    if (!this.metodosPago().some(metodo => metodo.codigo === this.formularioPago.metodoPago)) {
      this.formularioPago.metodoPago = this.metodosPago()[0]?.codigo ?? '';
    }
    if (!this.metodosPago().some(metodo => metodo.codigo === this.formularioMovimiento.metodoPago)) {
      this.formularioMovimiento.metodoPago = this.metodosPago()[0]?.codigo ?? '';
    }
    if (!this.tiposMovimiento().some(tipo => tipo.codigo === this.formularioMovimiento.tipoMovimiento)) {
      this.formularioMovimiento.tipoMovimiento = this.tiposMovimiento()[0]?.codigo ?? '';
    }
    const sucursalOperativa = catalogo.sucursalActivaId
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
    this.sucursalActivaId.set(sucursalId);
    this.sucursalSeleccionadaModel = sucursalId;
  }

  private sincronizarCitaSeleccionada() {
    const citas = this.citasPorCobrar();
    if (!citas.length) {
      this.citaSeleccionadaId.set(null);
      this.pagosCitaSeleccionada.set([]);
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

  private formatearMoneda(valor: number): string {
    return new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: 'MXN'
    }).format(valor || 0);
  }

  private formatearFechaHora(valor: string): string {
    return new Intl.DateTimeFormat('es-MX', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(valor));
  }
}
