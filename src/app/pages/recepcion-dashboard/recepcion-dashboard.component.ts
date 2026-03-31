import { CommonModule, DatePipe } from '@angular/common';
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
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject, forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import {
  CatalogoRecepcion,
  CitaRecepcion,
  ClienteRecepcion,
  RecepcionService,
  ServicioRecepcionCatalogo,
  SucursalRecepcionCatalogo
} from '../../core/recepcion/recepcion.service';

@Component({
  selector: 'app-recepcion-dashboard',
  standalone: true,
  imports: [
    CommonModule,
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
  templateUrl: './recepcion-dashboard.component.html',
  styleUrls: ['./recepcion-dashboard.component.css']
})
export class RecepcionDashboardComponent implements OnInit {
  private readonly recepcionService = inject(RecepcionService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly ngZone = inject(NgZone);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly busquedaCliente$ = new Subject<string>();

  readonly loading = signal(false);
  readonly loadingBusqueda = signal(false);
  readonly guardando = signal(false);
  readonly panelMovil = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly fechaAgenda = signal(this.fechaHoy());
  readonly sucursalActivaId = signal<number | null>(null);
  readonly citas = signal<CitaRecepcion[]>([]);
  readonly clientesEncontrados = signal<ClienteRecepcion[]>([]);
  readonly sucursales = signal<SucursalRecepcionCatalogo[]>([]);
  readonly servicios = signal<ServicioRecepcionCatalogo[]>([]);
  readonly terminoBusquedaCliente = signal('');

  readonly nombreUsuario = computed(() => this.authService.nombreUsuarioVisible());
  readonly correoUsuario = computed(() => this.authService.sesionActual()?.correo ?? '');
  readonly inicialesUsuario = computed(() => this.authService.inicialesUsuarioVisible());
  readonly puedeIrCaja = computed(() => this.authService.puedeVerCaja());
  readonly puedeIrAdmin = computed(() => this.authService.puedeVerAdmin());
  readonly puedeBuscarClientes = computed(() => this.authService.puedeBuscarClientesRecepcion());
  readonly puedeGestionarCitas = computed(() => this.authService.puedeGestionarRecepcionCitas());
  readonly puedeRegistrarCheckIn = computed(() => this.authService.puedeRegistrarCheckInRecepcion());
  readonly sucursalesPermitidas = computed(() => this.authService.sucursalesPermitidas());
  readonly sucursalActiva = computed(() =>
    this.sucursales().find(sucursal => sucursal.id === this.sucursalActivaId()) ?? this.sucursales()[0] ?? null
  );
  readonly sucursalActivaNombre = computed(() => this.sucursalActiva()?.nombre ?? 'Selecciona una sucursal');
  readonly metricas = computed(() => {
    const citas = this.citas();
    return [
      { label: 'Citas del día', value: citas.length },
      { label: 'Pendientes', value: citas.filter(cita => cita.estado === 'PENDIENTE').length },
      { label: 'Confirmadas', value: citas.filter(cita => cita.estado === 'CONFIRMADA').length },
      { label: 'Con check-in', value: citas.filter(cita => !!cita.checkInEn).length }
    ];
  });

  formularioCita = {
    sucursalId: null as number | null,
    servicioId: null as number | null,
    prestadorId: null as number | null,
    nombreCliente: '',
    correoCliente: '',
    telefonoCliente: '',
    inicio: '',
    notas: ''
  };

  constructor() {
    this.busquedaCliente$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        takeUntilDestroyed()
      )
      .subscribe(texto => {
        if (texto.trim().length < 2) {
          this.actualizarVistaEnZona(() => {
            this.clientesEncontrados.set([]);
          });
          return;
        }

        this.loadingBusqueda.set(true);
        this.recepcionService.buscarClientes(texto.trim())
          .pipe(finalize(() => this.loadingBusqueda.set(false)))
          .subscribe({
            next: clientes => this.actualizarVistaEnZona(() => this.clientesEncontrados.set(clientes)),
            error: () => this.actualizarVistaEnZona(() => this.clientesEncontrados.set([]))
          });
      });
  }

  ngOnInit(): void {
    this.authService.sincronizarSesionPersistida();
    const sucursalInicial = this.authService.sucursalesPermitidas()[0] ?? null;
    if (sucursalInicial) {
      this.sucursalActivaId.set(sucursalInicial);
      this.formularioCita.sucursalId = sucursalInicial;
    }

    this.breakpointObserver
      .observe('(max-width: 991px)')
      .pipe(takeUntilDestroyed())
      .subscribe(({ matches }) => this.actualizarVistaEnZona(() => this.panelMovil.set(matches)));

    this.cargarCatalogosYAgenda();
  }

  private actualizarVistaEnZona(actualizacion: () => void) {
    this.ngZone.run(() => {
      actualizacion();
      this.changeDetectorRef.detectChanges();
    });
  }

  cargarCatalogosYAgenda() {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      catalogo: this.recepcionService.getCatalogo(this.sucursalActivaId()),
      agenda: this.recepcionService.getAgenda(this.fechaAgenda(), this.sucursalActivaId())
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ catalogo, agenda }) => {
          this.actualizarVistaEnZona(() => {
            this.aplicarCatalogo(catalogo);
            this.citas.set(agenda);
          });
        },
        error: error => {
          this.actualizarVistaEnZona(() => {
            this.error.set(this.extraerMensaje(error, 'No pude cargar la agenda de recepción.'));
          });
        }
      });
  }

  recargarAgenda() {
    this.loading.set(true);
    this.error.set('');

    this.recepcionService.getAgenda(this.fechaAgenda(), this.sucursalActivaId())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: agenda => this.actualizarVistaEnZona(() => this.citas.set(agenda)),
        error: error => this.actualizarVistaEnZona(() => this.error.set(this.extraerMensaje(error, 'No pude actualizar la agenda.')))
      });
  }

  cambiarSucursal(sucursalId: number | null) {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      catalogo: this.recepcionService.getCatalogo(sucursalId),
      agenda: this.recepcionService.getAgenda(this.fechaAgenda(), sucursalId)
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ catalogo, agenda }) => this.actualizarVistaEnZona(() => {
          this.aplicarCatalogo(catalogo);
          this.citas.set(agenda);
        }),
        error: error => this.actualizarVistaEnZona(() => {
          this.error.set(this.extraerMensaje(error, 'No pude cambiar la sucursal de recepción.'));
        })
      });
  }

  cambiarFecha(fecha: string) {
    this.fechaAgenda.set(fecha);
    this.recargarAgenda();
  }

  buscarCliente(texto: string) {
    this.terminoBusquedaCliente.set(texto);
    this.busquedaCliente$.next(texto);
  }

  seleccionarCliente(cliente: ClienteRecepcion) {
    this.formularioCita.nombreCliente = cliente.nombreCompleto;
    this.formularioCita.correoCliente = cliente.correo;
    this.formularioCita.telefonoCliente = cliente.telefono;
    this.clientesEncontrados.set([]);
    this.terminoBusquedaCliente.set(cliente.nombreCompleto);
  }

  guardarCita() {
    if (!this.formularioCita.sucursalId || !this.formularioCita.servicioId || !this.formularioCita.inicio) {
      this.error.set('Completa sucursal, servicio y fecha/hora para registrar la cita.');
      return;
    }

    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.recepcionService.crearCita({
      sucursalId: this.formularioCita.sucursalId,
      servicioId: this.formularioCita.servicioId,
      prestadorId: this.formularioCita.prestadorId,
      nombreCliente: this.formularioCita.nombreCliente.trim(),
      correoCliente: this.formularioCita.correoCliente.trim(),
      telefonoCliente: this.formularioCita.telefonoCliente.trim(),
      inicio: new Date(this.formularioCita.inicio).toISOString(),
      notas: this.formularioCita.notas.trim() || null
    })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: respuesta => {
          this.mensaje.set(respuesta.mensaje);
          this.formularioCita = {
            sucursalId: this.sucursalActivaId(),
            servicioId: null,
            prestadorId: null,
            nombreCliente: '',
            correoCliente: '',
            telefonoCliente: '',
            inicio: '',
            notas: ''
          };
          this.recargarAgenda();
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude crear la cita desde recepción.'));
        }
      });
  }

  hacerCheckIn(citaId: number) {
    this.ejecutarAccion(() => this.recepcionService.checkIn(citaId), 'Check-in realizado.');
  }

  confirmar(citaId: number) {
    this.ejecutarAccion(() => this.recepcionService.confirmar(citaId), 'Cita confirmada.');
  }

  cancelar(citaId: number) {
    this.ejecutarAccion(() => this.recepcionService.cancelar(citaId), 'Cita cancelada.');
  }

  finalizar(citaId: number) {
    this.ejecutarAccion(() => this.recepcionService.finalizar(citaId), 'Cita finalizada.');
  }

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  irACaja() {
    this.router.navigateByUrl('/caja');
  }

  irAAdmin() {
    this.router.navigateByUrl('/admin');
  }

  private ejecutarAccion(accion: () => ReturnType<RecepcionService['confirmar']>, mensajeExito: string) {
    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');

    accion()
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: () => {
          this.mensaje.set(mensajeExito);
          this.recargarAgenda();
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude completar la acción solicitada.'));
        }
      });
  }

  private aplicarCatalogo(catalogo: CatalogoRecepcion) {
    const sucursalJwt = this.authService.sucursalesPermitidas()[0] ?? null;
    this.sucursales.set(catalogo.sucursales ?? []);
    this.servicios.set(catalogo.servicios ?? []);

    const sucursalOperativa = catalogo.sucursalActivaId
      ?? sucursalJwt
      ?? this.sucursalActivaId()
      ?? catalogo.sucursales[0]?.id
      ?? null;

    this.sucursalActivaId.set(sucursalOperativa);
    this.formularioCita.sucursalId = sucursalOperativa;

    if (
      this.formularioCita.servicioId &&
      !(catalogo.servicios ?? []).some(servicio => servicio.id === this.formularioCita.servicioId)
    ) {
      this.formularioCita.servicioId = null;
    }
  }

  private fechaHoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private extraerMensaje(error: unknown, fallback: string): string {
    const httpError = error as { error?: { message?: string }; message?: string };
    return httpError?.error?.message || httpError?.message || fallback;
  }
}
