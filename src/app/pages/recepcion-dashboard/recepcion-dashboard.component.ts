import { CommonModule, DatePipe } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, NgZone, OnInit, ViewEncapsulation, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Observable, Subject, forkJoin, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, finalize, switchMap, tap } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { PerfilUsuarioLocal, UserProfileService } from '../../core/profile/user-profile.service';
import { UserProfileDialogComponent } from '../../shared/profile/user-profile-dialog.component';
import { RecepcionSidePanelComponent } from './recepcion-side-panel.component';
import { RecepcionAgendaSectionComponent } from './agenda/recepcion-agenda-section.component';
import { FormularioCitaRecepcion } from './forms/recepcion.forms';
import {
  CatalogoRecepcion,
  CitaRecepcion,
  ClienteRecepcion,
  FranjaRecepcionDisponible,
  RecepcionService,
  SolicitudEsperaRecepcion,
  ServicioRecepcionCatalogo,
  SucursalRecepcionCatalogo,
  OpcionRecepcion
} from '../../core/recepcion/recepcion.service';

@Component({
  selector: 'app-recepcion-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    MatBadgeModule,
    MatButtonModule,
    MatMenuModule,
    MatProgressBarModule,
    MatToolbarModule,
    UserProfileDialogComponent,
    RecepcionSidePanelComponent,
    RecepcionAgendaSectionComponent
  ],
  templateUrl: './recepcion-dashboard.component.html',
  styleUrls: ['./recepcion-dashboard.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class RecepcionDashboardComponent implements OnInit, AfterViewInit {
  private readonly recepcionService = inject(RecepcionService);
  private readonly authService = inject(AuthService);
  private readonly userProfileService = inject(UserProfileService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly ngZone = inject(NgZone);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly busquedaCliente$ = new Subject<string>();
  private reintentoInicialProgramado = false;

  readonly loading = signal(false);
  readonly loadingBusqueda = signal(false);
  readonly guardando = signal(false);
  readonly panelMovil = signal(false);
  readonly perfilConfigAbierto = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly fechaAgenda = signal(this.fechaHoy());
  readonly sucursalActivaId = signal<number | null>(null);
  readonly citas = signal<CitaRecepcion[]>([]);
  readonly clientesEncontrados = signal<ClienteRecepcion[]>([]);
  readonly sucursales = signal<SucursalRecepcionCatalogo[]>([]);
  readonly servicios = signal<ServicioRecepcionCatalogo[]>([]);
  readonly solicitudesEspera = signal<SolicitudEsperaRecepcion[]>([]);
  readonly terminoBusquedaCliente = signal('');
  readonly loadingFranjas = signal(false);
  readonly franjasDisponibles = signal<FranjaRecepcionDisponible[]>([]);
  readonly mensajeWalkIn = signal('');
  readonly estadosCita = signal<OpcionRecepcion[]>([]);
  readonly estadoCitaPendiente = signal('');
  readonly estadoCitaConfirmada = signal('');
  readonly estadosCitaFinalizables = signal<string[]>([]);
  readonly estadosCitaCancelables = signal<string[]>([]);
  readonly estadosEspera = signal<OpcionRecepcion[]>([]);
  readonly estadoEsperaPendiente = signal('');
  readonly estadoEsperaNotificada = signal('');

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
    || 'Recepción'
  );
  readonly fotoPerfilUsuario = computed(() => this.perfilUsuario().fotoDataUrl);
  readonly inicialesUsuario = computed(() => this.obtenerIniciales(this.nombreUsuario()));
  readonly puedeIrCaja = computed(() => this.authService.puedeVerCaja());
  readonly puedeIrAdmin = computed(() => this.authService.puedeVerAdmin());
  readonly puedeBuscarClientes = computed(() => this.authService.puedeBuscarClientesRecepcion());
  readonly puedeGestionarCitas = computed(() => this.authService.puedeGestionarRecepcionCitas());
  readonly puedeRegistrarCheckIn = computed(() => this.authService.puedeRegistrarCheckInRecepcion());
  readonly sucursalesPermitidas = computed(() => this.authService.sucursalesPermitidas());
  readonly sucursalActiva = computed(() =>
    this.sucursales().find(sucursal => sucursal.id === this.sucursalActivaId()) ?? this.sucursales()[0] ?? null
  );
  readonly sucursalActivaNombre = computed(() => {
    const sucursal = this.sucursalActiva();
    if (sucursal?.nombre) {
      return sucursal.nombre;
    }
    const desdeAgenda = this.citas()[0]?.sucursalNombre?.trim();
    if (desdeAgenda) {
      return desdeAgenda;
    }
    const desdeJwt = this.sucursalesPermitidas()[0];
    if (desdeJwt) {
      return `Sucursal ${desdeJwt}`;
    }
    return 'Selecciona una sucursal';
  });
  readonly metricas = computed(() => {
    const citas = this.citas();
    return [
      { label: 'Citas del día', value: citas.length },
      { label: 'Pendientes', value: citas.filter(cita => cita.estado === this.estadoCitaPendiente()).length },
      { label: 'Confirmadas', value: citas.filter(cita => cita.estado === this.estadoCitaConfirmada()).length },
      { label: 'Con check-in', value: citas.filter(cita => !!cita.checkInEn).length }
    ];
  });
  readonly totalNotificaciones = computed(() =>
    this.citas().filter(cita => cita.estado === this.estadoCitaPendiente() || !cita.checkInEn).length
  );
  readonly notificacionesRecepcion = computed(() => {
    const pendientes = this.citas().filter(cita => cita.estado === this.estadoCitaPendiente());
    const sinCheckIn = this.citas().filter(cita => !cita.checkInEn);
    return [
      pendientes.length ? { titulo: 'Citas pendientes', descripcion: `${pendientes.length} citas necesitan seguimiento.`, icono: 'bi-calendar-check', total: pendientes.length } : null,
      sinCheckIn.length ? { titulo: 'Check-in pendiente', descripcion: `${sinCheckIn.length} clientes aún no tienen check-in.`, icono: 'bi-person-check', total: sinCheckIn.length } : null
    ].filter((item): item is { titulo: string; descripcion: string; icono: string; total: number } => !!item);
  });

  formularioCita: FormularioCitaRecepcion = {
    sucursalId: null as number | null,
    servicioId: null as number | null,
    clienteId: null as number | null,
    prestadorId: null as number | null,
    nombreCliente: '',
    correoCliente: '',
    telefonoCliente: '',
    fechaWalkIn: this.fechaHoy(),
    inicio: '',
    notas: '',
    avisarWhatsapp: true
  };

  constructor() {
    this.busquedaCliente$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
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
    this.userProfileService.cargar();
    const sucursalInicial = this.authService.sucursalesPermitidas()[0] ?? null;
    if (sucursalInicial) {
      this.sucursalActivaId.set(sucursalInicial);
      this.formularioCita.sucursalId = sucursalInicial;
    }
    this.formularioCita.fechaWalkIn = this.fechaAgenda();

    this.breakpointObserver
      .observe('(max-width: 991px)')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ matches }) => this.actualizarVistaEnZona(() => this.panelMovil.set(matches)));

    this.cargarCatalogosYAgenda();
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
      if (this.contextoInicialIncompleto()) {
        this.cargarCatalogosYAgenda();
      }
    }, 180);
  }

  private contextoInicialIncompleto(): boolean {
    return !this.loading() && (
      !this.sucursalActivaId()
      || !this.sucursales().length
      || (this.puedeGestionarCitas() && !this.servicios().length)
      || (!this.citas().length && !this.error())
    );
  }

  cargarCatalogosYAgenda() {
    this.loading.set(true);
    this.error.set('');
    const sucursalPreferida = this.sucursalActivaId();

    this.recepcionService.getCatalogo(sucursalPreferida)
      .pipe(
        catchError(() => of<CatalogoRecepcion>({
          sucursalActivaId: sucursalPreferida,
          sucursales: [],
          servicios: [],
          estadosCita: [],
          estadoCitaPendiente: '',
          estadoCitaConfirmada: '',
          estadosCitaFinalizables: [],
          estadosCitaCancelables: [],
          estadosEspera: [],
          estadoEsperaPendiente: '',
          estadoEsperaNotificada: ''
        })),
        tap(catalogo => {
          this.actualizarVistaEnZona(() => {
            this.aplicarCatalogo(catalogo);
          });
        }),
        switchMap(catalogo => {
          const sucursalId = catalogo.sucursalActivaId ?? this.sucursalActivaId();
          return forkJoin({
            agenda: this.cargarAgenda$(sucursalId),
            espera: this.cargarSolicitudesEspera$(sucursalId)
          });
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({ agenda, espera }) => {
          this.actualizarVistaEnZona(() => {
            this.citas.set(agenda);
            this.solicitudesEspera.set(espera);
            this.aplicarContextoDesdeAgenda(agenda);
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

    const sucursalConsulta = this.sucursalActivaId() ?? this.sucursalesPermitidas()[0] ?? null;

    this.recepcionService.getAgenda(this.fechaAgenda(), sucursalConsulta)
      .pipe(
        switchMap(agenda => forkJoin({
          agenda: of(agenda),
          espera: this.cargarSolicitudesEspera$(sucursalConsulta)
        })),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({ agenda, espera }) => this.actualizarVistaEnZona(() => {
          this.citas.set(agenda);
          this.solicitudesEspera.set(espera);
          this.aplicarContextoDesdeAgenda(agenda);
        }),
        error: error => this.actualizarVistaEnZona(() => this.error.set(this.extraerMensaje(error, 'No pude actualizar la agenda.')))
      });
  }

  cambiarSucursal(sucursalId: number | null) {
    this.loading.set(true);
    this.error.set('');

    this.recepcionService.getCatalogo(sucursalId)
      .pipe(
        catchError(() => of<CatalogoRecepcion>({
          sucursalActivaId: sucursalId,
          sucursales: [],
          servicios: [],
          estadosCita: [],
          estadoCitaPendiente: '',
          estadoCitaConfirmada: '',
          estadosCitaFinalizables: [],
          estadosCitaCancelables: [],
          estadosEspera: [],
          estadoEsperaPendiente: '',
          estadoEsperaNotificada: ''
        })),
        tap(catalogo => {
          this.actualizarVistaEnZona(() => {
            this.aplicarCatalogo(catalogo);
          });
        }),
        switchMap(catalogo => {
          const sucursalConsulta = catalogo.sucursalActivaId ?? sucursalId;
          return forkJoin({
            agenda: this.cargarAgenda$(sucursalConsulta),
            espera: this.cargarSolicitudesEspera$(sucursalConsulta)
          });
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({ agenda, espera }) => this.actualizarVistaEnZona(() => {
          this.citas.set(agenda);
          this.solicitudesEspera.set(espera);
          this.aplicarContextoDesdeAgenda(agenda);
        }),
        error: error => this.actualizarVistaEnZona(() => {
          this.error.set(this.extraerMensaje(error, 'No pude cambiar la sucursal de recepción.'));
        })
      });
  }

  cambiarFecha(fecha: string) {
    this.fechaAgenda.set(fecha);
    if (!this.formularioCita.fechaWalkIn) {
      this.formularioCita.fechaWalkIn = fecha;
    }
    this.recargarAgenda();
  }

  actualizarServicioWalkIn(servicioId: number | null) {
    this.formularioCita.servicioId = servicioId;
    this.formularioCita.prestadorId = null;
    this.formularioCita.inicio = '';
    this.franjasDisponibles.set([]);
    this.mensajeWalkIn.set('');

    if (this.formularioCita.sucursalId && servicioId) {
      this.cargarFranjasWalkIn();
    }
  }

  actualizarFechaWalkIn(fecha: string) {
    this.formularioCita.fechaWalkIn = fecha;
    this.formularioCita.prestadorId = null;
    this.formularioCita.inicio = '';
    this.franjasDisponibles.set([]);
    this.mensajeWalkIn.set('');

    if (this.formularioCita.sucursalId && this.formularioCita.servicioId) {
      this.cargarFranjasWalkIn();
    }
  }

  seleccionarFranjaWalkIn(franja: FranjaRecepcionDisponible) {
    this.formularioCita.inicio = franja.inicio;
    this.formularioCita.prestadorId = franja.prestadorId;
    this.mensajeWalkIn.set(`Turno seleccionado: ${franja.hora}.`);
  }

  cargarFranjasWalkIn() {
    const sucursalId = this.formularioCita.sucursalId ?? this.sucursalActivaId();
    const servicioId = this.formularioCita.servicioId;
    const fecha = this.formularioCita.fechaWalkIn || this.fechaAgenda();

    this.formularioCita.inicio = '';
    this.formularioCita.prestadorId = null;
    this.franjasDisponibles.set([]);
    this.mensajeWalkIn.set('');

    if (!sucursalId || !servicioId) {
      return;
    }

    this.loadingFranjas.set(true);
    this.recepcionService.getFranjasDisponibles(sucursalId, servicioId, fecha)
      .pipe(finalize(() => this.loadingFranjas.set(false)))
      .subscribe({
        next: franjas => this.actualizarVistaEnZona(() => {
          this.franjasDisponibles.set(franjas);
          if (!franjas.length) {
            this.mensajeWalkIn.set(
              'No hay horarios disponibles para esta fecha. Podemos dejar al cliente en espera y avisarle por WhatsApp con plantilla aprobada cuando se libere un espacio.'
            );
            return;
          }
          this.seleccionarFranjaWalkIn(franjas[0]);
        }),
        error: error => this.actualizarVistaEnZona(() => {
          this.error.set(this.extraerMensaje(error, 'No pude consultar horarios disponibles para walk-ins.'));
        })
      });
  }

  buscarCliente(texto: string) {
    this.terminoBusquedaCliente.set(texto);
    this.busquedaCliente$.next(texto);
  }

  seleccionarCliente(cliente: ClienteRecepcion) {
    this.formularioCita.clienteId = cliente.usuarioId;
    this.formularioCita.nombreCliente = cliente.nombreCompleto;
    this.formularioCita.correoCliente = cliente.correo;
    this.formularioCita.telefonoCliente = cliente.telefono;
    this.formularioCita.avisarWhatsapp = cliente.aceptaWhatsapp;
    this.clientesEncontrados.set([]);
    this.terminoBusquedaCliente.set(cliente.nombreCompleto);
  }

  guardarCita() {
    if (!this.formularioCita.sucursalId || !this.formularioCita.servicioId || !this.formularioCita.inicio) {
      this.error.set('Selecciona sucursal, servicio y una franja disponible para registrar la cita.');
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
            clienteId: null,
            prestadorId: null,
            nombreCliente: '',
            correoCliente: '',
            telefonoCliente: '',
            fechaWalkIn: this.fechaAgenda(),
            inicio: '',
            notas: '',
            avisarWhatsapp: true
          };
          this.franjasDisponibles.set([]);
          this.mensajeWalkIn.set('');
          this.recargarAgenda();
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude crear la cita desde recepción.'));
        }
      });
  }

  registrarEspera() {
    if (!this.formularioCita.sucursalId || !this.formularioCita.servicioId) {
      this.error.set('Selecciona sucursal y servicio para registrar la espera.');
      return;
    }

    if (!this.formularioCita.nombreCliente.trim() || !this.formularioCita.telefonoCliente.trim()) {
      this.error.set('Necesito al menos nombre y teléfono para registrar al cliente en espera.');
      return;
    }

    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.recepcionService.registrarEspera({
      sucursalId: this.formularioCita.sucursalId,
      servicioId: this.formularioCita.servicioId,
      clienteId: this.formularioCita.clienteId,
      nombreCliente: this.formularioCita.nombreCliente.trim(),
      telefonoCliente: this.formularioCita.telefonoCliente.trim(),
      fechaDeseada: this.formularioCita.fechaWalkIn || this.fechaAgenda(),
      horaDesde: null,
      horaHasta: null,
      aceptaWhatsapp: this.formularioCita.avisarWhatsapp,
      canalOrigen: 'MOSTRADOR',
      notas: this.formularioCita.notas.trim() || null
    })
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: solicitud => {
          this.mensaje.set(
            solicitud.aceptaWhatsapp
              ? 'Cliente registrado en espera. Queda listo para notificarle por WhatsApp cuando se libere un espacio.'
              : 'Cliente registrado en espera para seguimiento desde recepción.'
          );
          this.solicitudesEspera.update(actuales => [solicitud, ...actuales]);
          this.formularioCita = {
            sucursalId: this.sucursalActivaId(),
            servicioId: null,
            clienteId: null,
            prestadorId: null,
            nombreCliente: '',
            correoCliente: '',
            telefonoCliente: '',
            fechaWalkIn: this.fechaAgenda(),
            inicio: '',
            notas: '',
            avisarWhatsapp: true
          };
          this.franjasDisponibles.set([]);
          this.mensajeWalkIn.set('');
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude registrar al cliente en espera.'));
        }
      });
  }

  notificarEspera(solicitudId: number) {
    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.recepcionService.notificarEspera(solicitudId)
      .pipe(finalize(() => this.guardando.set(false)))
      .subscribe({
        next: solicitudActualizada => {
          this.solicitudesEspera.update(actuales =>
            actuales.map(item => item.id === solicitudActualizada.id ? solicitudActualizada : item)
          );
          this.mensaje.set('Se notificó al cliente por WhatsApp usando la plantilla configurada para esta empresa.');
        },
        error: error => {
          this.error.set(this.extraerMensaje(error, 'No pude enviar la notificación de WhatsApp para esta solicitud.'));
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

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  irACaja() {
    this.ngZone.run(() => {
      void this.router.navigateByUrl('/caja');
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
    this.estadosCita.set(catalogo.estadosCita ?? []);
    this.estadoCitaPendiente.set(catalogo.estadoCitaPendiente ?? '');
    this.estadoCitaConfirmada.set(catalogo.estadoCitaConfirmada ?? '');
    this.estadosCitaFinalizables.set(catalogo.estadosCitaFinalizables ?? []);
    this.estadosCitaCancelables.set(catalogo.estadosCitaCancelables ?? []);
    this.estadosEspera.set(catalogo.estadosEspera ?? []);
    this.estadoEsperaPendiente.set(catalogo.estadoEsperaPendiente ?? '');
    this.estadoEsperaNotificada.set(catalogo.estadoEsperaNotificada ?? '');

    const sucursalOperativa = catalogo.sucursalActivaId
      ?? sucursalJwt
      ?? this.sucursalActivaId()
      ?? catalogo.sucursales[0]?.id
      ?? null;

    this.sucursalActivaId.set(sucursalOperativa);
    this.formularioCita.sucursalId = sucursalOperativa;
    this.formularioCita.fechaWalkIn = this.formularioCita.fechaWalkIn || this.fechaAgenda();

    if (
      this.formularioCita.servicioId &&
      !(catalogo.servicios ?? []).some(servicio => servicio.id === this.formularioCita.servicioId)
    ) {
      this.formularioCita.servicioId = null;
      this.formularioCita.inicio = '';
      this.formularioCita.prestadorId = null;
      this.franjasDisponibles.set([]);
    }

    if (!this.formularioCita.servicioId && (catalogo.servicios ?? []).length === 1) {
      this.formularioCita.servicioId = catalogo.servicios[0].id;
    }

    this.sincronizarContextoOperativo();

    if (this.formularioCita.sucursalId && this.formularioCita.servicioId) {
      this.cargarFranjasWalkIn();
    }
  }

  private cargarAgenda$(sucursalId?: number | null): Observable<CitaRecepcion[]> {
    return this.recepcionService.getAgenda(this.fechaAgenda(), sucursalId);
  }

  private cargarSolicitudesEspera$(sucursalId?: number | null): Observable<SolicitudEsperaRecepcion[]> {
    return this.recepcionService.getSolicitudesEspera(this.fechaAgenda(), sucursalId)
      .pipe(catchError(() => of([])));
  }

  private aplicarContextoDesdeAgenda(agenda: CitaRecepcion[]) {
    if (!this.sucursales().length && agenda.length) {
      const sucursalesInferidas = Array.from(new Map(
        agenda
          .filter(cita => !!cita.sucursalId)
          .map(cita => [cita.sucursalId, {
            id: cita.sucursalId,
            empresaId: this.authService.sesionActual()?.empresaId ?? 0,
            nombre: cita.sucursalNombre,
            direccion: '',
            telefono: '',
            zonaHoraria: 'America/Mexico_City'
          } satisfies SucursalRecepcionCatalogo])
      ).values());
      this.sucursales.set(sucursalesInferidas);
    }

    if (!this.servicios().length && agenda.length) {
      const serviciosInferidos = Array.from(new Map(
        agenda
          .filter(cita => !!cita.servicioId)
          .map(cita => [cita.servicioId, {
            id: cita.servicioId,
            sucursalId: cita.sucursalId,
            nombre: cita.servicioNombre,
            descripcion: '',
            duracionMinutos: 60,
            bufferAntesMinutos: 0,
            bufferDespuesMinutos: 0,
            precio: Number(cita.precio ?? 0),
            moneda: cita.moneda ?? 'MXN'
          } satisfies ServicioRecepcionCatalogo])
      ).values());
      this.servicios.set(serviciosInferidos);
    }

    this.sincronizarContextoOperativo(agenda);

    if (!this.formularioCita.servicioId && this.servicios().length === 1) {
      this.formularioCita.servicioId = this.servicios()[0].id;
    }

    if (
      this.formularioCita.sucursalId &&
      this.formularioCita.servicioId &&
      !this.franjasDisponibles().length &&
      !this.loadingFranjas()
    ) {
      this.cargarFranjasWalkIn();
    }
  }

  private sincronizarContextoOperativo(agenda: CitaRecepcion[] = this.citas()) {
    const sucursalActualId = this.sucursalActivaId()
      ?? this.formularioCita.sucursalId
      ?? agenda[0]?.sucursalId
      ?? this.sucursales()[0]?.id
      ?? this.authService.sucursalesPermitidas()[0]
      ?? null;

    if (sucursalActualId) {
      this.sucursalActivaId.set(sucursalActualId);
      this.formularioCita.sucursalId = sucursalActualId;
    }

    if (!this.sucursales().length && sucursalActualId) {
      const nombreAgenda = agenda.find(cita => cita.sucursalId === sucursalActualId)?.sucursalNombre?.trim();
      this.sucursales.set([
        {
          id: sucursalActualId,
          empresaId: this.authService.sesionActual()?.empresaId ?? 0,
          nombre: nombreAgenda || `Sucursal ${sucursalActualId}`,
          direccion: '',
          telefono: '',
          zonaHoraria: 'America/Mexico_City'
        }
      ]);
    }

    if (!this.servicios().length && agenda.length) {
      const serviciosInferidos = Array.from(new Map(
        agenda
          .filter(cita => cita.sucursalId === this.formularioCita.sucursalId && !!cita.servicioId)
          .map(cita => [cita.servicioId, {
            id: cita.servicioId,
            sucursalId: cita.sucursalId,
            nombre: cita.servicioNombre,
            descripcion: '',
            duracionMinutos: 60,
            bufferAntesMinutos: 0,
            bufferDespuesMinutos: 0,
            precio: Number(cita.precio ?? 0),
            moneda: cita.moneda ?? 'MXN'
          } satisfies ServicioRecepcionCatalogo])
      ).values());
      this.servicios.set(serviciosInferidos);
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
