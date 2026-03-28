import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import {
  CajaService,
  CajaSesion,
  CitaPorCobrar,
  MovimientoCaja,
  PagoCita,
  ResumenCaja,
  SucursalCaja
} from '../../core/caja/caja.service';

type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA';
type TipoMovimiento = 'GASTO_MENOR' | 'RETIRO' | 'INGRESO_EXTRA' | 'AJUSTE_POSITIVO' | 'AJUSTE_NEGATIVO';

@Component({
  selector: 'app-caja-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatProgressBarModule,
    MatSelectModule,
    MatToolbarModule
  ],
  templateUrl: './caja-dashboard.component.html',
  styleUrls: ['./caja-dashboard.component.css']
})
export class CajaDashboardComponent implements OnInit {
  private readonly cajaService = inject(CajaService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly ngZone = inject(NgZone);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  readonly loading = signal(false);
  readonly guardandoApertura = signal(false);
  readonly guardandoCierre = signal(false);
  readonly guardandoPago = signal(false);
  readonly guardandoMovimiento = signal(false);
  readonly panelMovil = signal(false);
  readonly sucursales = signal<SucursalCaja[]>([]);
  readonly sucursalActivaId = signal<number | null>(null);
  readonly sesionActual = signal<CajaSesion | null>(null);
  readonly citasPorCobrar = signal<CitaPorCobrar[]>([]);
  readonly resumen = signal<ResumenCaja | null>(null);
  readonly pagosCitaSeleccionada = signal<PagoCita[]>([]);
  readonly ultimoMovimiento = signal<MovimientoCaja | null>(null);
  readonly citaSeleccionadaId = signal<number | null>(null);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly metodosPago: MetodoPago[] = ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA'];
  readonly tiposMovimiento: Array<{ value: TipoMovimiento; label: string }> = [
    { value: 'GASTO_MENOR', label: 'Gasto menor' },
    { value: 'RETIRO', label: 'Retiro de efectivo' },
    { value: 'INGRESO_EXTRA', label: 'Ingreso extra' },
    { value: 'AJUSTE_POSITIVO', label: 'Ajuste positivo' },
    { value: 'AJUSTE_NEGATIVO', label: 'Ajuste negativo' }
  ];

  readonly nombreUsuario = computed(() => this.authService.nombreUsuarioVisible());
  readonly correoUsuario = computed(() => this.authService.sesionActual()?.correo ?? '');
  readonly inicialesUsuario = computed(() => this.authService.inicialesUsuarioVisible());
  readonly puedeIrRecepcion = computed(() => this.authService.puedeVerRecepcion());
  readonly puedeIrAdmin = computed(() => this.authService.puedeVerAdmin());
  readonly cajaAbierta = computed(() => this.sesionActual()?.estado === 'ABIERTA');
  readonly sucursalActiva = computed(() =>
    this.sucursales().find(sucursal => sucursal.id === this.sucursalActivaId()) ?? this.sucursales()[0] ?? null
  );
  readonly sucursalActivaNombre = computed(() => this.sucursalActiva()?.nombre ?? 'Selecciona una sucursal');
  readonly sucursalActivaDireccion = computed(() => this.sucursalActiva()?.direccion ?? '');
  readonly citaSeleccionada = computed(() =>
    this.citasPorCobrar().find(cita => cita.citaId === this.citaSeleccionadaId()) ?? this.citasPorCobrar()[0] ?? null
  );
  readonly citaSeleccionadaActualId = computed(() => this.citaSeleccionada()?.citaId ?? null);
  readonly totalPendiente = computed(() =>
    this.citasPorCobrar().reduce((total, cita) => total + (Number(cita.pendiente) || 0), 0)
  );
  readonly metricas = computed(() => [
    { label: 'Pendiente por cobrar', value: this.totalPendiente(), accent: 'primary' },
    { label: 'Cobrado en turno', value: Number(this.resumen()?.totalCobrado ?? 0), accent: 'neutral' },
    { label: 'Efectivo esperado', value: Number(this.resumen()?.saldoEsperadoCaja ?? 0), accent: 'neutral' },
    { label: 'Citas por cobrar', value: this.citasPorCobrar().length, accent: 'soft' }
  ]);

  formularioApertura = {
    montoInicial: 0,
    observaciones: ''
  };

  formularioCierre = {
    montoContado: 0,
    observaciones: ''
  };

  formularioPago = {
    monto: 0,
    metodoPago: 'EFECTIVO' as MetodoPago,
    referencia: '',
    observaciones: ''
  };

  formularioMovimiento = {
    tipoMovimiento: 'GASTO_MENOR' as TipoMovimiento,
    monto: 0,
    metodoPago: 'EFECTIVO' as MetodoPago,
    concepto: '',
    referencia: '',
    observaciones: ''
  };

  ngOnInit(): void {
    this.breakpointObserver
      .observe('(max-width: 991px)')
      .pipe(takeUntilDestroyed())
      .subscribe(({ matches }) => {
        this.actualizarVistaEnZona(() => {
          this.panelMovil.set(matches);
        });
      });

    this.cargarSucursales();
  }

  cargarSucursales() {
    this.loading.set(true);
    this.error.set('');

    this.cajaService.getSucursales()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: sucursales => {
          this.sucursales.set(sucursales);
          if (!this.sucursalActivaId() && sucursales.length) {
            this.sucursalActivaId.set(sucursales[0].id);
          }
          this.recargarTablero();
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude cargar las sucursales para Caja.'));
        }
      });
  }

  recargarTablero() {
    const sucursalId = this.sucursalActivaId();
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      sesion: this.cajaService.getSesionActual(sucursalId),
      citas: this.cajaService.getCitasPorCobrar(sucursalId),
      resumen: this.cajaService.getResumen(sucursalId)
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ sesion, citas, resumen }) => {
          this.sesionActual.set(sesion);
          this.citasPorCobrar.set(citas);
          this.resumen.set(resumen);
          this.sincronizarCitaSeleccionada();
          this.formularioCierre.montoContado = Number(resumen.saldoEsperadoCaja ?? sesion?.montoEsperado ?? 0);
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude actualizar el tablero de Caja.'));
        }
      });
  }

  seleccionarSucursal(sucursalId: number | null) {
    this.sucursalActivaId.set(sucursalId);
    this.citaSeleccionadaId.set(null);
    this.pagosCitaSeleccionada.set([]);
    this.recargarTablero();
  }

  abrirCaja() {
    if (!this.sucursalActivaId()) {
      this.error.set('Selecciona una sucursal antes de abrir la caja.');
      return;
    }

    this.guardandoApertura.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.cajaService.abrirCaja({
      sucursalId: this.sucursalActivaId()!,
      montoInicial: Number(this.formularioApertura.montoInicial),
      observaciones: this.formularioApertura.observaciones.trim() || null
    })
      .pipe(finalize(() => this.guardandoApertura.set(false)))
      .subscribe({
        next: sesion => {
          this.sesionActual.set(sesion);
          this.formularioApertura = { montoInicial: 0, observaciones: '' };
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

    this.cajaService.cerrarCaja(sesion.id, {
      montoContado: Number(this.formularioCierre.montoContado),
      observaciones: this.formularioCierre.observaciones.trim() || null
    })
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
    }

    this.cajaService.listarPagosCita(citaId).subscribe({
      next: pagos => {
        this.pagosCitaSeleccionada.set(pagos);
      },
      error: error => {
        this.error.set(this.extraerMensaje(error, 'No pude cargar el historial de pagos de la cita.'));
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

    this.cajaService.registrarPago(cita.citaId, {
      monto: Number(this.formularioPago.monto),
      metodoPago: this.formularioPago.metodoPago,
      referencia: this.formularioPago.referencia.trim() || null,
      observaciones: this.formularioPago.observaciones.trim() || null
    })
      .pipe(finalize(() => this.guardandoPago.set(false)))
      .subscribe({
        next: pago => {
          this.mensaje.set('Pago registrado correctamente.');
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
    if (!this.sucursalActivaId()) {
      this.error.set('Selecciona una sucursal antes de registrar movimientos de caja.');
      return;
    }

    this.guardandoMovimiento.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.cajaService.registrarMovimiento({
      sucursalId: this.sucursalActivaId()!,
      tipoMovimiento: this.formularioMovimiento.tipoMovimiento,
      monto: Number(this.formularioMovimiento.monto),
      metodoPago: this.formularioMovimiento.metodoPago,
      concepto: this.formularioMovimiento.concepto.trim(),
      referencia: this.formularioMovimiento.referencia.trim() || null,
      observaciones: this.formularioMovimiento.observaciones.trim() || null
    })
      .pipe(finalize(() => this.guardandoMovimiento.set(false)))
      .subscribe({
        next: movimiento => {
          this.ultimoMovimiento.set(movimiento);
          this.formularioMovimiento = {
            tipoMovimiento: 'GASTO_MENOR',
            monto: 0,
            metodoPago: 'EFECTIVO',
            concepto: '',
            referencia: '',
            observaciones: ''
          };
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

  irARecepcion() {
    this.router.navigateByUrl('/recepcion');
  }

  irAAdmin() {
    this.router.navigateByUrl('/admin');
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
    }
  }

  private actualizarVistaEnZona(actualizacion: () => void) {
    this.ngZone.run(() => {
      actualizacion();
      this.changeDetectorRef.detectChanges();
    });
  }

  private extraerMensaje(error: unknown, fallback: string): string {
    const httpError = error as { error?: { message?: string }; message?: string };
    return httpError?.error?.message || httpError?.message || fallback;
  }
}
