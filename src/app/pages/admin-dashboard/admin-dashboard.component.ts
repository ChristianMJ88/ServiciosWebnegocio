import { CommonModule, CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { AgendaOperationsSectionComponent } from '../../components/agenda/agenda-operations-section.component';
import { buildWhatsAppUrl, formatAgendaStatusLabel, getDurationMinutes, getInitials } from '../../components/agenda/agenda.helpers';
import {
  AgendaActionVm,
  AgendaAppointmentVm,
  AgendaCollaboratorVm,
  AgendaOccupancyVm,
  AgendaStatCardVm
} from '../../components/agenda/agenda.types';
import {
  AdminService,
  ConfiguracionCorreoAdmin,
  ConfiguracionWhatsappAdmin,
  DetectarChannelSenderWhatsappResponse,
  ExcepcionDisponibilidadAdmin,
  GuardarConfiguracionCorreoPayload,
  GuardarConfiguracionWhatsappPayload,
  GuardarExcepcionDisponibilidadPayload,
  GuardarPrestadorPayload,
  GuardarReglaDisponibilidadPayload,
  GuardarServicioPayload,
  GuardarSucursalPayload,
  GuardarUsuarioInternoPayload,
  LogMensajeWhatsappAdmin,
  MigracionSecretosCorreoResponse,
  PlantillaWhatsappAdmin,
  PrestadorAdmin,
  AsociarChannelSenderWhatsappPayload,
  AsociarChannelSenderWhatsappResponse,
  ProvisionarMessagingServiceWhatsappPayload,
  ProvisionarMessagingServiceWhatsappResponse,
  ProvisionarSubcuentaWhatsappPayload,
  ProvisionarSubcuentaWhatsappResponse,
  ProbarPlantillaWhatsappPayload,
  PruebaWhatsappResponse,
  ReporteServicioAdmin,
  ReglaDisponibilidadAdmin,
  ResumenAdmin,
  SolicitudContactoAdmin,
  ServicioAdmin,
  SucursalAdmin,
  UsuarioInternoAdmin
} from '../../core/admin/admin.service';
import { AuthService } from '../../core/auth/auth.service';
import { CitaCliente } from '../../core/auth/client-appointments.service';

type SeccionAdmin =
  | 'resumen'
  | 'correo'
  | 'whatsapp'
  | 'contactos'
  | 'sucursales'
  | 'servicios'
  | 'usuarios'
  | 'prestadores'
  | 'reglas'
  | 'excepciones'
  | 'citas';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    FormsModule,
    MatBadgeModule,
    MatToolbarModule,
    MatListModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatCheckboxModule,
    MatChipsModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    MatProgressBarModule,
    AgendaOperationsSectionComponent
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly inicioAgendaHora = 8;
  private readonly finAgendaHora = 20;
  private readonly alturaHoraAgenda = 86;
  readonly loading = signal(false);
  readonly fechaAgendaSeleccionada = signal<string | null>(null);
  readonly citaAgendaSeleccionadaId = signal<number | null>(null);
  readonly seccionActiva = signal<SeccionAdmin>('resumen');
  readonly panelMovil = signal(false);
  readonly sidebarAbierto = signal(true);
  readonly sidebarCompacto = signal(false);
  readonly resumen = signal<ResumenAdmin | null>(null);
  readonly citas = signal<CitaCliente[]>([]);
  readonly contactos = signal<SolicitudContactoAdmin[]>([]);
  readonly sucursales = signal<SucursalAdmin[]>([]);
  readonly servicios = signal<ServicioAdmin[]>([]);
  readonly prestadores = signal<PrestadorAdmin[]>([]);
  readonly usuariosInternos = signal<UsuarioInternoAdmin[]>([]);
  readonly reglasDisponibilidad = signal<ReglaDisponibilidadAdmin[]>([]);
  readonly excepcionesDisponibilidad = signal<ExcepcionDisponibilidadAdmin[]>([]);
  readonly reporteServicios = signal<ReporteServicioAdmin[]>([]);
  readonly configuracionCorreo = signal<ConfiguracionCorreoAdmin | null>(null);
  readonly configuracionWhatsapp = signal<ConfiguracionWhatsappAdmin | null>(null);
  readonly plantillasWhatsapp = signal<PlantillaWhatsappAdmin[]>([]);
  readonly logsWhatsapp = signal<LogMensajeWhatsappAdmin[]>([]);
  readonly whatsappOnboardingChecklist = computed(() => {
    const config = this.configuracionWhatsapp();
    const plantillas = this.plantillasWhatsapp();
    const tipoCuenta = config?.tipoCuentaTwilio ?? 'PLATAFORMA';
    const tieneCredenciales = !!config?.accountSid && !!config?.authTokenConfigurado;
    const subcuentaLista = tipoCuenta !== 'SUBCUENTA' || !!config?.subaccountSid;
    const messagingServiceListo = !!config?.messagingServiceSid;
    const channelSenderListo = !!config?.channelSenderSid;
    const remitenteListo = !!config?.numeroRemitente;
    const plantillasListas = plantillas.length > 0
      || !!config?.plantillaSolicitudConfirmacionSid
      || !!config?.plantillaReprogramadaPendienteSid
      || !!config?.plantillaRecordatorioConfirmacionSid
      || !!config?.plantillaCitaConfirmadaSid
      || !!config?.plantillaRecordatorioSid
      || !!config?.plantillaCancelacionSid
      || !!config?.plantillaLiberadaSinConfirmacionSid
      || !!config?.plantillaGraciasVisitaSid
      || !!config?.plantillaRecordatorioRegresoSid;

    return [
      {
        key: 'modelo',
        done: !!tipoCuenta,
        title: 'Modelo de cuenta definido',
        detail:
          tipoCuenta === 'SUBCUENTA'
            ? 'El tenant operará en una subcuenta Twilio aislada.'
            : tipoCuenta === 'CUENTA_PROPIA'
              ? 'El tenant usará sus propias credenciales de Twilio.'
              : 'El tenant opera con la cuenta plataforma.'
      },
      {
        key: 'credenciales',
        done: tieneCredenciales,
        title: 'Credenciales operativas listas',
        detail: tieneCredenciales
          ? 'El tenant ya tiene Account SID y Auth Token disponibles.'
          : 'Falta guardar credenciales válidas de Twilio para operar.'
      },
      {
        key: 'subcuenta',
        done: subcuentaLista,
        title: 'Subcuenta provisionada',
        detail:
          tipoCuenta === 'SUBCUENTA'
            ? (config?.subaccountSid
              ? `Subcuenta registrada: ${config.subaccountSid}.`
              : 'Aún falta crear o capturar la subcuenta del tenant.')
            : 'No aplica para este modelo de cuenta.'
      },
      {
        key: 'remitente',
        done: remitenteListo,
        title: 'Número remitente capturado',
        detail: remitenteListo
          ? `Remitente configurado: ${config?.numeroRemitente}.`
          : 'Falta capturar el número remitente de WhatsApp.'
      },
      {
        key: 'messaging',
        done: messagingServiceListo,
        title: 'Messaging Service listo',
        detail: messagingServiceListo
          ? `Messaging Service activo: ${config?.messagingServiceSid}.`
          : 'Aún no se ha creado o capturado el Messaging Service SID.'
      },
      {
        key: 'sender',
        done: channelSenderListo,
        title: 'Sender asociado al servicio',
        detail: channelSenderListo
          ? `Channel Sender detectado: ${config?.channelSenderSid}.`
          : 'Falta detectar o asociar el Channel Sender SID (XE...).'
      },
      {
        key: 'plantillas',
        done: plantillasListas,
        title: 'Plantillas disponibles',
        detail: plantillasListas
          ? `${plantillas.length || [config?.plantillaSolicitudConfirmacionSid, config?.plantillaReprogramadaPendienteSid, config?.plantillaRecordatorioConfirmacionSid, config?.plantillaCitaConfirmadaSid, config?.plantillaRecordatorioSid, config?.plantillaCancelacionSid, config?.plantillaLiberadaSinConfirmacionSid, config?.plantillaGraciasVisitaSid, config?.plantillaRecordatorioRegresoSid].filter(Boolean).length} plantilla(s) visibles para el tenant.`
          : 'Aún no hay plantillas detectadas o configuradas.'
      },
      {
        key: 'canal',
        done: !!config?.habilitado,
        title: 'Canal habilitado',
        detail: config?.habilitado
          ? 'WhatsApp ya está habilitado para este tenant.'
          : 'El canal sigue deshabilitado aunque la configuración exista.'
      }
    ];
  });
  readonly whatsappOnboardingStats = computed(() => {
    const checklist = this.whatsappOnboardingChecklist();
    const completados = checklist.filter(item => item.done).length;
    const total = checklist.length;
    const porcentaje = total ? Math.round((completados / total) * 100) : 0;
    const siguiente = checklist.find(item => !item.done) ?? null;

    return {
      completados,
      total,
      porcentaje,
      siguiente
    };
  });
  readonly diasSemana = [
    { value: 1, label: 'Lunes' },
    { value: 2, label: 'Martes' },
    { value: 3, label: 'Miércoles' },
    { value: 4, label: 'Jueves' },
    { value: 5, label: 'Viernes' },
    { value: 6, label: 'Sábado' },
    { value: 7, label: 'Domingo' }
  ];
  readonly diasSemanaTexto: Record<number, string> = {
    1: 'Lunes',
    2: 'Martes',
    3: 'Miércoles',
    4: 'Jueves',
    5: 'Viernes',
    6: 'Sábado',
    7: 'Domingo'
  };
  readonly tiposBloqueo = ['BLOQUEO', 'DESCANSO', 'VACACIONES', 'HORARIO_ESPECIAL'];
  readonly modulosAdmin: Array<{ id: SeccionAdmin; titulo: string; descripcion: string; abreviatura: string; icono: string }> = [
    { id: 'resumen', titulo: 'Resumen ejecutivo', descripcion: 'Métricas generales y desempeño comercial.', abreviatura: 'RE', icono: 'resumen' },
    { id: 'correo', titulo: 'Correo transaccional', descripcion: 'Graph o SMTP por tenant, con cifrado y migración de secretos.', abreviatura: 'CO', icono: 'correo' },
    { id: 'whatsapp', titulo: 'WhatsApp y Twilio', descripcion: 'Sender, plantillas, pruebas y trazabilidad por tenant.', abreviatura: 'WA', icono: 'whatsapp' },
    { id: 'contactos', titulo: 'Contactos', descripcion: 'Mensajes recibidos desde el formulario web y seguimiento comercial.', abreviatura: 'CN', icono: 'contactos' },
    { id: 'sucursales', titulo: 'Sucursales', descripcion: 'Alta y mantenimiento de sedes operativas.', abreviatura: 'SU', icono: 'sucursal' },
    { id: 'servicios', titulo: 'Servicios', descripcion: 'Catálogo, duración, buffers y precio.', abreviatura: 'SV', icono: 'servicio' },
    { id: 'usuarios', titulo: 'Usuarios internos', descripcion: 'Recepción, caja y administradores internos.', abreviatura: 'UI', icono: 'prestador' },
    { id: 'prestadores', titulo: 'Prestadores', descripcion: 'Usuarios staff y asignaciones de servicio.', abreviatura: 'PR', icono: 'prestador' },
    { id: 'reglas', titulo: 'Horarios base', descripcion: 'Reglas semanales por sucursal o prestador.', abreviatura: 'HB', icono: 'horario' },
    { id: 'excepciones', titulo: 'Bloqueos', descripcion: 'Vacaciones, descansos y cierres puntuales.', abreviatura: 'BL', icono: 'bloqueo' },
    { id: 'citas', titulo: 'Citas', descripcion: 'Seguimiento operativo de reservas creadas.', abreviatura: 'CT', icono: 'agenda' }
  ];
  readonly moduloActivo = computed(() => this.modulosAdmin.find(modulo => modulo.id === this.seccionActiva()) ?? this.modulosAdmin[0]);
  readonly nombreUsuarioAdmin = computed(() => this.authService.nombreUsuarioVisible());
  readonly correoUsuarioAdmin = computed(() => this.authService.sesionActual()?.correo ?? '');
  readonly inicialesUsuarioAdmin = computed(() => this.authService.inicialesUsuarioVisible());
  readonly totalNotificaciones = computed(() =>
    (this.resumen()?.pendientes ?? 0) + this.contactos().filter(contacto => contacto.estado === 'NUEVO').length
  );
  readonly resumenContactos = computed(() => ({
    total: this.contactos().length,
    nuevos: this.contactos().filter(contacto => contacto.estado === 'NUEVO').length,
    enProceso: this.contactos().filter(contacto => contacto.estado === 'EN_PROCESO').length,
    atendidos: this.contactos().filter(contacto => contacto.estado === 'ATENDIDO').length
  }));
  readonly estadosContactoDisponibles = ['NUEVO', 'EN_PROCESO', 'ATENDIDO', 'CERRADO'];
  readonly rolesUsuarioInterno = [
    { codigo: 'ADMIN', nombre: 'Administrador' },
    { codigo: 'RECEPCIONISTA', nombre: 'Recepcionista' },
    { codigo: 'CAJERO', nombre: 'Caja' }
  ];
  readonly sucursalActivaNombre = computed(() => this.sucursales()[0]?.nombre ?? 'Sucursal principal');
  readonly horasAgenda = Array.from({ length: this.finAgendaHora - this.inicioAgendaHora + 1 }, (_, index) => {
    const hora = this.inicioAgendaHora + index;
    return {
      hora,
      etiqueta: `${hora.toString().padStart(2, '0')}:00`
    };
  });
  readonly citasAgrupadas = computed(() => {
    const agrupadas = new Map<string, {
      fechaClave: string;
      fechaTexto: string;
      items: Array<CitaCliente & { horaTexto: string; rangoTexto: string }>;
    }>();

    const citasOrdenadas = [...this.citas()].sort((a, b) => new Date(a.inicio).getTime() - new Date(b.inicio).getTime());
    for (const cita of citasOrdenadas) {
      const inicio = new Date(cita.inicio);
      const fin = new Date(cita.fin);
      const fechaClave = cita.inicio.slice(0, 10);
      const existente = agrupadas.get(fechaClave);
      const item = {
        ...cita,
        horaTexto: inicio.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' }),
        rangoTexto: `${inicio.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' })} - ${fin.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' })}`
      };

      if (existente) {
        existente.items.push(item);
        continue;
      }

      agrupadas.set(fechaClave, {
        fechaClave,
        fechaTexto: inicio.toLocaleDateString('es-MX', {
          weekday: 'long',
          day: 'numeric',
          month: 'long'
        }),
        items: [item]
      });
    }

    return Array.from(agrupadas.values());
  });
  readonly fechasConCitasAgenda = computed(() =>
    Array.from(new Set(this.citas().map(cita => cita.inicio.slice(0, 10)))).sort((a, b) => a.localeCompare(b))
  );
  readonly fechaAgendaActiva = computed(() => {
    return this.fechaAgendaSeleccionada() ?? this.obtenerFechaAgendaInicial();
  });
  readonly fechaAgendaActivaTexto = computed(() => this.formatearFechaAgendaLarga(this.fechaAgendaActiva()));
  readonly citasDelDiaActivas = computed(() => {
    const fecha = this.fechaAgendaActiva();
    return this.citasAgrupadas().find(grupo => grupo.fechaClave === fecha)?.items ?? [];
  });
  readonly resumenAgendaActiva = computed(() => {
    const citas = this.citas().filter(cita => cita.inicio.startsWith(this.fechaAgendaActiva()));
    return {
      total: citas.length,
      pendientes: citas.filter(cita => cita.estado === 'PENDIENTE').length,
      confirmadas: citas.filter(cita => cita.estado === 'CONFIRMADA').length,
      colaboradores: new Set(citas.map(cita => cita.prestadorId)).size
    };
  });
  readonly analiticaAgendaActiva = computed(() => {
    const citas = this.citasDelDiaActivas();
    const minutosReservados = citas.reduce((total, cita) => total + getDurationMinutes(cita.inicio, cita.fin), 0);
    const capacidadMinutos = Math.max(this.colaboradoresAgenda().length, 1) * (this.finAgendaHora - this.inicioAgendaHora) * 60;
    const ocupacion = capacidadMinutos ? Math.min(100, Math.round((minutosReservados / capacidadMinutos) * 100)) : 0;

    return {
      ocupacion,
      horasReservadas: Math.max(0, Math.round((minutosReservados / 60) * 10) / 10),
      horasLibres: Math.max(0, Math.round(((capacidadMinutos - minutosReservados) / 60) * 10) / 10)
    };
  });
  readonly colaboradoresAgenda = computed(() => {
    const fecha = this.fechaAgendaActiva();
    const mapa = new Map<number, { id: number; nombre: string }>();

    for (const cita of this.citas()) {
      if (!cita.inicio.startsWith(fecha)) {
        continue;
      }
      mapa.set(cita.prestadorId, { id: cita.prestadorId, nombre: cita.prestadorNombre });
    }

    if (!mapa.size) {
      for (const prestador of this.prestadores().slice(0, 4)) {
        mapa.set(prestador.usuarioId, { id: prestador.usuarioId, nombre: prestador.nombreMostrar });
      }
    }

    return Array.from(mapa.values());
  });
  readonly agendaHourLabels = this.horasAgenda.map(hora => hora.etiqueta);
  readonly anchoAgendaTimeline = computed(() => Math.max(this.colaboradoresAgenda().length * 272, 860));
  readonly citasAgendaPosicionadas = computed(() => {
    const fecha = this.fechaAgendaActiva();
    const columnas = this.colaboradoresAgenda();
    const totalColumnas = Math.max(columnas.length, 1);
    const anchoColumna = 100 / totalColumnas;
    const indiceColumna = new Map(columnas.map((columna, index) => [columna.id, index]));
    const citasDelDia = this.citas().filter(cita => cita.inicio.startsWith(fecha));
    const distribucionPorPrestador = new Map<number, Map<CitaCliente, { lane: number; laneCount: number }>>();

    for (const colaborador of columnas) {
      const citasColumna = citasDelDia.filter(cita => cita.prestadorId === colaborador.id);
      distribucionPorPrestador.set(colaborador.id, this.calcularDistribucionAgenda(citasColumna));
    }

    return citasDelDia.map(cita => {
        const inicio = new Date(cita.inicio);
        const fin = new Date(cita.fin);
        const minutosInicio = (inicio.getHours() - this.inicioAgendaHora) * 60 + inicio.getMinutes();
        const duracionMinutos = Math.max(30, Math.round((fin.getTime() - inicio.getTime()) / 60000));
        const top = (minutosInicio / 60) * this.alturaHoraAgenda;
        const height = Math.max(64, (duracionMinutos / 60) * this.alturaHoraAgenda - 8);
        const distribucion = distribucionPorPrestador.get(cita.prestadorId)?.get(cita) ?? { lane: 0, laneCount: 1 };
        const columna = indiceColumna.get(cita.prestadorId) ?? 0;
        const widthPct = anchoColumna / distribucion.laneCount;
        const leftPct = columna * anchoColumna + distribucion.lane * widthPct;

        return {
          ...cita,
          top,
          height,
          columna,
          leftPct,
          widthPct
        };
      });
  });
  readonly resumenAgendaCards = computed<AgendaStatCardVm[]>(() => [
    { label: 'Citas del día', value: this.resumenAgendaActiva().total },
    { label: 'Pendientes', value: this.resumenAgendaActiva().pendientes },
    { label: 'Confirmadas', value: this.resumenAgendaActiva().confirmadas },
    { label: 'Colaboradores', value: this.resumenAgendaActiva().colaboradores }
  ]);
  readonly agendaOccupancyVm = computed<AgendaOccupancyVm>(() => ({
    percent: this.analiticaAgendaActiva().ocupacion,
    bookedLabel: `${this.analiticaAgendaActiva().horasReservadas} h reservadas`,
    freeLabel: `${this.analiticaAgendaActiva().horasLibres} h libres`,
    helper: 'Ocupación operativa'
  }));
  readonly agendaCollaboratorsVm = computed<AgendaCollaboratorVm[]>(() =>
    this.colaboradoresAgenda().map(colaborador => ({
      id: colaborador.id,
      name: colaborador.nombre,
      meta: `${this.totalCitasColaborador(colaborador.id)} citas asignadas`,
      avatar: getInitials(colaborador.nombre),
      accentColor: this.prestadores().find(prestador => prestador.usuarioId === colaborador.id)?.colorAgenda ?? null
    }))
  );
  readonly agendaAppointmentsVm = computed<AgendaAppointmentVm[]>(() =>
    this.citasAgendaPosicionadas().map(cita => {
      const clienteNombre = cita.clienteNombre || `Reserva #${cita.id}`;
      const whatsappUrl = buildWhatsAppUrl(cita.clienteTelefono);

      return {
        id: cita.id,
        status: cita.estado,
        statusLabel: formatAgendaStatusLabel(cita.estado),
        title: cita.servicioNombre,
        subtitle: clienteNombre,
        supportingText: `${cita.prestadorNombre} · ${cita.sucursalNombre}`,
        supportingTextSecondary: cita.clienteTelefono || cita.clienteCorreo || `Reserva #${cita.id}`,
        priceLabel: this.formatearMonedaAgenda(cita.precio, cita.moneda),
        avatarLabel: getInitials(clienteNombre),
        top: cita.top,
        height: cita.height,
        leftPct: cita.leftPct,
        widthPct: cita.widthPct,
        start: cita.inicio,
        end: cita.fin,
        detailTitle: clienteNombre,
        detailEyebrow: 'Reserva seleccionada',
        notes: cita.notas,
        metaFields: [
          { label: 'Servicio', value: cita.servicioNombre },
          { label: 'Prestador', value: cita.prestadorNombre },
          { label: 'Sucursal', value: cita.sucursalNombre },
          { label: 'Estado', value: formatAgendaStatusLabel(cita.estado) },
          { label: 'Precio', value: this.formatearMonedaAgenda(cita.precio, cita.moneda) },
          { label: 'Contacto', value: cita.clienteTelefono || cita.clienteCorreo || 'Sin contacto' }
        ],
        actions: this.construirAccionesAgendaAdmin(cita.id, cita.estado, whatsappUrl)
      };
    })
  );
  readonly citaAgendaSeleccionada = computed<AgendaAppointmentVm | null>(() => {
    const citas = this.agendaAppointmentsVm();
    if (!citas.length) {
      return null;
    }

    return citas.find(cita => cita.id === this.citaAgendaSeleccionadaId()) ?? citas[0];
  });
  error = '';
  mensajeExito = '';
  guardandoSucursal = false;
  guardandoServicio = false;
  guardandoPrestador = false;
  guardandoRegla = false;
  guardandoExcepcion = false;
  guardandoCorreo = false;
  guardandoWhatsapp = false;
  provisionandoSubcuentaWhatsapp = false;
  provisionandoMessagingServiceWhatsapp = false;
  asociandoChannelSenderWhatsapp = false;
  detectandoChannelSenderWhatsapp = false;
  actualizandoContactoId: number | null = null;
  migrandoSecretosCorreo = false;
  probandoPlantillaWhatsapp = false;
  sujetosRegla: Array<{ id: number; nombre: string }> = [];
  sujetosExcepcion: Array<{ id: number; nombre: string }> = [];
  serviciosPrestadorDisponibles: ServicioAdmin[] = [];
  sucursalEditandoId: number | null = null;
  servicioEditandoId: number | null = null;
  prestadorEditandoId: number | null = null;
  usuarioInternoEditandoId: number | null = null;
  reglaEditandoId: number | null = null;
  excepcionEditandoId: number | null = null;

  formularioSucursal: GuardarSucursalPayload = {
    nombre: '',
    direccion: '',
    telefono: '',
    zonaHoraria: 'America/Mexico_City',
    activa: true
  };

  formularioServicio: GuardarServicioPayload = {
    sucursalId: 0,
    nombre: '',
    descripcion: '',
    duracionMinutos: 60,
    bufferAntesMinutos: 0,
    bufferDespuesMinutos: 0,
    precio: 0,
    moneda: 'MXN',
    activo: true
  };

  formularioPrestador: GuardarPrestadorPayload = {
    sucursalId: 0,
    correo: '',
    contrasenaTemporal: '',
    nombreMostrar: '',
    biografia: '',
    colorAgenda: '#2563eb',
    activo: true,
    servicioIds: []
  };

  formularioUsuarioInterno: GuardarUsuarioInternoPayload = {
    sucursalId: null,
    correo: '',
    contrasenaTemporal: '',
    nombreCompleto: '',
    telefono: '',
    puesto: '',
    rolCodigo: 'RECEPCIONISTA',
    activo: true,
    notas: ''
  };

  formularioRegla: GuardarReglaDisponibilidadPayload = {
    tipoSujeto: 'SUCURSAL',
    sujetoId: 0,
    diaSemana: 1,
    horaInicio: '09:00',
    horaFin: '18:00',
    intervaloMinutos: 15,
    vigenteDesde: null,
    vigenteHasta: null
  };

  formularioExcepcion: GuardarExcepcionDisponibilidadPayload = {
    tipoSujeto: 'SUCURSAL',
    sujetoId: 0,
    fechaExcepcion: '',
    horaInicio: null,
    horaFin: null,
    tipoBloqueo: 'BLOQUEO',
    motivo: null
  };

  formularioCorreo: GuardarConfiguracionCorreoPayload = {
    habilitado: false,
    proveedor: 'SMTP',
    remitente: '',
    nombreRemitente: '',
    responderA: '',
    smtpHost: '',
    smtpPort: 587,
    smtpUsername: '',
    smtpPassword: '',
    smtpAuth: true,
    smtpStartTls: true,
    graphTenantId: '',
    graphClientId: '',
    graphClientSecret: '',
    graphUserId: '',
    graphCertificateThumbprint: '',
    graphPrivateKeyPem: ''
  };

  formularioWhatsapp: GuardarConfiguracionWhatsappPayload = {
    habilitado: false,
    accountSid: '',
    authToken: '',
    tipoCuentaTwilio: 'PLATAFORMA',
    subaccountSid: '',
    numeroRemitente: '',
    messagingServiceSid: '',
    channelSenderSid: '',
    statusCallbackUrl: '',
    plantillaSolicitudConfirmacionSid: '',
    plantillaReprogramadaPendienteSid: '',
    plantillaRecordatorioConfirmacionSid: '',
    plantillaCitaConfirmadaSid: '',
    plantillaRecordatorioSid: '',
    plantillaCancelacionSid: '',
    plantillaLiberadaSinConfirmacionSid: '',
    plantillaGraciasVisitaSid: '',
    plantillaRecordatorioRegresoSid: '',
    senderDisplayName: '',
    senderPhoneNumber: '',
    senderStatus: '',
    qualityRating: '',
    throughputMps: null,
    wabaId: '',
    metaBusinessManagerId: ''
  };

  formularioPruebaWhatsapp: ProbarPlantillaWhatsappPayload = {
    telefonoDestino: '',
    nombreCliente: '',
    fecha: '',
    hora: '',
    plantillaSid: null
  };

  formularioProvisionSubcuentaWhatsapp: ProvisionarSubcuentaWhatsappPayload = {
    friendlyName: ''
  };

  formularioProvisionMessagingServiceWhatsapp: ProvisionarMessagingServiceWhatsappPayload = {
    friendlyName: '',
    inboundRequestUrl: ''
  };

  formularioAsociacionChannelSenderWhatsapp: AsociarChannelSenderWhatsappPayload = {
    channelSenderSid: ''
  };

  ngOnInit(): void {
    this.actualizarVistaEnZona(() => {
      this.sincronizarSidebarConViewport(this.breakpointObserver.isMatched('(max-width: 991px)'));
    });

    if (this.authService.asegurarSesion()) {
      this.recargar();
    }

    this.breakpointObserver
      .observe('(max-width: 991px)')
      .pipe(takeUntilDestroyed())
      .subscribe(({ matches }) => {
        this.actualizarVistaEnZona(() => {
          this.sincronizarSidebarConViewport(matches);
        });
      });
  }

  private actualizarVistaEnZona(actualizacion: () => void) {
    this.ngZone.run(() => {
      actualizacion();
      this.changeDetectorRef.detectChanges();
    });
  }

  private sincronizarSidebarConViewport(esMovil: boolean) {
    this.panelMovil.set(esMovil);
    this.sidebarAbierto.set(!esMovil);
    if (esMovil) {
      this.sidebarCompacto.set(false);
    }
  }

  seleccionarSeccion(seccion: SeccionAdmin) {
    this.seccionActiva.set(seccion);
    if (this.panelMovil()) {
      this.sidebarAbierto.set(false);
    }
  }

  alternarSidebar() {
    this.sidebarAbierto.update(valor => !valor);
  }

  cerrarSidebar() {
    this.sidebarAbierto.set(false);
  }

  alternarCompactoSidebar() {
    if (this.panelMovil()) {
      this.sidebarAbierto.update(valor => !valor);
      return;
    }
    this.sidebarCompacto.update(valor => !valor);
  }

  tituloSeccionActual(): string {
    return this.modulosAdmin.find(modulo => modulo.id === this.seccionActiva())?.titulo ?? 'Panel Admin';
  }

  descripcionSeccionActual(): string {
    return this.modulosAdmin.find(modulo => modulo.id === this.seccionActiva())?.descripcion ?? '';
  }

  claseEstadoCita(estado: string): string {
    return `estado-${estado.toLowerCase()}`;
  }

  formatearEstadoCita(estado: string): string {
    return formatAgendaStatusLabel(estado);
  }

  obtenerInicialesNombre(nombre: string): string {
    return getInitials(nombre);
  }

  totalCitasColaborador(colaboradorId: number): number {
    return this.citasDelDiaActivas().filter(cita => cita.prestadorId === colaboradorId).length;
  }

  seleccionarCitaAgenda(citaId: number): void {
    this.citaAgendaSeleccionadaId.set(citaId);
  }

  gestionarAccionAgenda(evento: { actionId: string; appointmentId: number }): void {
    switch (evento.actionId) {
      case 'confirm':
        this.actualizarEstadoAgenda(() => this.adminService.confirmarCita(evento.appointmentId));
        break;
      case 'finalize':
        this.actualizarEstadoAgenda(() => this.adminService.finalizarCita(evento.appointmentId));
        break;
      case 'no_show':
        this.actualizarEstadoAgenda(() => this.adminService.marcarNoAsistio(evento.appointmentId));
        break;
      case 'cancel':
        this.actualizarEstadoAgenda(() => this.adminService.cancelarCita(evento.appointmentId));
        break;
      default:
        break;
    }
  }

  private calcularDistribucionAgenda<T extends { inicio: string; fin: string }>(eventos: T[]) {
    const ordenados = [...eventos].sort((a, b) => new Date(a.inicio).getTime() - new Date(b.inicio).getTime());
    const distribucion = new Map<T, { lane: number; laneCount: number }>();
    const finPorCarril: number[] = [];

    for (const evento of ordenados) {
      const inicio = new Date(evento.inicio).getTime();
      const fin = new Date(evento.fin).getTime();
      let lane = finPorCarril.findIndex(finCarril => finCarril <= inicio);

      if (lane === -1) {
        lane = finPorCarril.length;
        finPorCarril.push(fin);
      } else {
        finPorCarril[lane] = fin;
      }

      const laneCount = Math.max(
        1,
        ordenados.filter(candidato => this.seSolapan(evento.inicio, evento.fin, candidato.inicio, candidato.fin)).length
      );

      distribucion.set(evento, { lane, laneCount });
    }

    return distribucion;
  }

  private seSolapan(inicioA: string, finA: string, inicioB: string, finB: string) {
    const desdeA = new Date(inicioA).getTime();
    const hastaA = new Date(finA).getTime();
    const desdeB = new Date(inicioB).getTime();
    const hastaB = new Date(finB).getTime();

    return desdeA < hastaB && hastaA > desdeB;
  }

  seleccionarFechaAgenda(fecha: string | null) {
    if (!fecha) {
      this.fechaAgendaSeleccionada.set(this.obtenerFechaAgendaInicial());
      this.citaAgendaSeleccionadaId.set(null);
      return;
    }
    this.fechaAgendaSeleccionada.set(fecha);
    this.citaAgendaSeleccionadaId.set(null);
  }

  desplazarFechaAgenda(dias: number) {
    this.fechaAgendaSeleccionada.set(this.sumarDiasAgenda(this.fechaAgendaActiva(), dias));
    this.citaAgendaSeleccionadaId.set(null);
  }

  irAHoyAgenda() {
    this.fechaAgendaSeleccionada.set(this.obtenerFechaLocalISO());
    this.citaAgendaSeleccionadaId.set(null);
  }

  logout() {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }

  recargar() {
    if (!this.authService.asegurarSesion()) {
      return;
    }

    this.actualizarVistaEnZona(() => {
      this.loading.set(true);
      this.error = '';
      this.mensajeExito = '';
    });
    forkJoin({
      resumen: this.adminService.getResumen().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudo cargar el resumen.');
          return of(null);
        })
      ),
      citas: this.adminService.getCitas().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar las citas.');
          return of([]);
        })
      ),
      contactos: this.adminService.getContactos().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar los contactos.');
          return of([]);
        })
      ),
      sucursales: this.adminService.getSucursales().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar las sucursales.');
          return of([]);
        })
      ),
      servicios: this.adminService.getServicios().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar los servicios.');
          return of([]);
        })
      ),
      prestadores: this.adminService.getPrestadores().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar los prestadores.');
          return of([]);
        })
      ),
      usuariosInternos: this.adminService.getUsuariosInternos().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar los usuarios internos.');
          return of([]);
        })
      ),
      reglas: this.adminService.getReglasDisponibilidad().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar las reglas de disponibilidad.');
          return of([]);
        })
      ),
      excepciones: this.adminService.getExcepcionesDisponibilidad().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar las excepciones.');
          return of([]);
        })
      ),
      reporteServicios: this.adminService.getReporteServicios().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudo cargar el reporte de servicios.');
          return of([]);
        })
      ),
      configuracionCorreo: this.adminService.getConfiguracionCorreo().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudo cargar la configuración de correo.');
          return of(null);
        })
      ),
      configuracionWhatsapp: this.adminService.getConfiguracionWhatsapp().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudo cargar la configuración de WhatsApp.');
          return of(null);
        })
      ),
      plantillasWhatsapp: this.adminService.getPlantillasWhatsapp().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar las plantillas de WhatsApp.');
          return of([]);
        })
      ),
      logsWhatsapp: this.adminService.getLogsWhatsapp().pipe(
        catchError(err => {
          this.marcarErrorCarga(err, 'No se pudieron cargar los logs de WhatsApp.');
          return of([]);
        })
      )
    })
      .pipe(finalize(() => this.actualizarVistaEnZona(() => this.loading.set(false))))
      .subscribe({
        next: ({ resumen, citas, contactos, sucursales, servicios, prestadores, usuariosInternos, reglas, excepciones, reporteServicios, configuracionCorreo, configuracionWhatsapp, plantillasWhatsapp, logsWhatsapp }) => {
          this.actualizarVistaEnZona(() => {
            this.resumen.set(resumen);
            this.citas.set(citas);
            this.contactos.set(contactos);
            this.sucursales.set(sucursales);
            this.servicios.set(servicios);
            this.prestadores.set(prestadores);
            this.usuariosInternos.set(usuariosInternos);
            this.reglasDisponibilidad.set(reglas);
            this.excepcionesDisponibilidad.set(excepciones);
            this.reporteServicios.set(reporteServicios);
            this.configuracionCorreo.set(configuracionCorreo);
            this.configuracionWhatsapp.set(configuracionWhatsapp);
            this.plantillasWhatsapp.set(plantillasWhatsapp);
            this.logsWhatsapp.set(logsWhatsapp);
            if (!this.formularioServicio.sucursalId && sucursales.length > 0) {
              this.formularioServicio.sucursalId = sucursales[0].id;
            }
            if (!this.formularioPrestador.sucursalId && sucursales.length > 0) {
              this.formularioPrestador.sucursalId = sucursales[0].id;
            }
            if (this.formularioUsuarioInterno.sucursalId === null && sucursales.length === 1) {
              this.formularioUsuarioInterno.sucursalId = sucursales[0].id;
            }
            this.sincronizarFormularioCorreo(configuracionCorreo);
            this.sincronizarFormularioWhatsapp(configuracionWhatsapp);
            this.actualizarServiciosPrestadorDisponibles();
            this.actualizarSujetosRegla();
            this.actualizarSujetosExcepcion();
            this.inicializarFechaAgenda();
            this.citaAgendaSeleccionadaId.set(null);
          });
        },
        error: err => {
          this.actualizarVistaEnZona(() => {
            this.error = err?.error?.mensaje || err?.message || 'No se pudo cargar el panel administrativo.';
          });
        }
      });
  }

  actualizarEstadoContacto(contacto: SolicitudContactoAdmin, estado: string) {
    if (this.actualizandoContactoId === contacto.id || contacto.estado === estado) {
      return;
    }

    this.actualizandoContactoId = contacto.id;
    this.error = '';
    this.mensajeExito = '';

    this.adminService.actualizarEstadoContacto(contacto.id, estado)
      .pipe(finalize(() => { this.actualizandoContactoId = null; }))
      .subscribe({
        next: contactoActualizado => {
          this.contactos.update(contactos =>
            contactos.map(item => item.id === contactoActualizado.id ? contactoActualizado : item)
          );
          this.mensajeExito = 'El estado del contacto se actualizó correctamente.';
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo actualizar el estado del contacto.';
        }
      });
  }

  claseEstadoContacto(estado: string): string {
    return `contacto-estado-${estado.toLowerCase()}`;
  }

  formatearEstadoContacto(estado: string): string {
    switch (estado) {
      case 'EN_PROCESO':
        return 'En proceso';
      case 'ATENDIDO':
        return 'Atendido';
      case 'CERRADO':
        return 'Cerrado';
      case 'NUEVO':
      default:
        return 'Nuevo';
    }
  }

  guardarConfiguracionCorreo() {
    this.guardandoCorreo = true;
    this.error = '';
    this.mensajeExito = '';
    const payload: GuardarConfiguracionCorreoPayload = {
      habilitado: this.formularioCorreo.habilitado,
      proveedor: this.formularioCorreo.proveedor,
      remitente: this.normalizarTexto(this.formularioCorreo.remitente),
      nombreRemitente: this.normalizarTexto(this.formularioCorreo.nombreRemitente),
      responderA: this.normalizarTexto(this.formularioCorreo.responderA),
      smtpHost: this.normalizarTexto(this.formularioCorreo.smtpHost),
      smtpPort: this.formularioCorreo.smtpPort ? Number(this.formularioCorreo.smtpPort) : null,
      smtpUsername: this.normalizarTexto(this.formularioCorreo.smtpUsername),
      smtpPassword: this.normalizarTexto(this.formularioCorreo.smtpPassword),
      smtpAuth: this.formularioCorreo.smtpAuth,
      smtpStartTls: this.formularioCorreo.smtpStartTls,
      graphTenantId: this.normalizarTexto(this.formularioCorreo.graphTenantId),
      graphClientId: this.normalizarTexto(this.formularioCorreo.graphClientId),
      graphClientSecret: this.normalizarTexto(this.formularioCorreo.graphClientSecret),
      graphUserId: this.normalizarTexto(this.formularioCorreo.graphUserId),
      graphCertificateThumbprint: this.normalizarTexto(this.formularioCorreo.graphCertificateThumbprint),
      graphPrivateKeyPem: this.normalizarTexto(this.formularioCorreo.graphPrivateKeyPem)
    };

    this.adminService.actualizarConfiguracionCorreo(payload)
      .pipe(finalize(() => this.guardandoCorreo = false))
      .subscribe({
        next: response => {
          this.configuracionCorreo.set(response);
          this.sincronizarFormularioCorreo(response);
          this.mensajeExito = 'La configuración de correo se guardó correctamente.';
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la configuración de correo.';
        }
      });
  }

  migrarSecretosCorreo() {
    this.migrandoSecretosCorreo = true;
    this.error = '';
    this.mensajeExito = '';
    this.adminService.migrarSecretosCorreo()
      .pipe(finalize(() => this.migrandoSecretosCorreo = false))
      .subscribe({
        next: (response: MigracionSecretosCorreoResponse) => {
          this.mensajeExito = response.mensaje;
          this.adminService.getConfiguracionCorreo().subscribe({
            next: configuracion => {
              this.configuracionCorreo.set(configuracion);
              this.sincronizarFormularioCorreo(configuracion);
            },
            error: err => {
              this.error = err?.error?.mensaje || err?.message || 'Se migró el secreto, pero no se pudo refrescar la configuración.';
            }
          });
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo migrar el secreto SMTP.';
        }
      });
  }

  guardarConfiguracionWhatsapp() {
    this.guardandoWhatsapp = true;
    this.error = '';
    this.mensajeExito = '';
    const payload: GuardarConfiguracionWhatsappPayload = {
      habilitado: this.formularioWhatsapp.habilitado,
      accountSid: this.normalizarTexto(this.formularioWhatsapp.accountSid),
      authToken: this.normalizarTexto(this.formularioWhatsapp.authToken),
      tipoCuentaTwilio: this.normalizarTexto(this.formularioWhatsapp.tipoCuentaTwilio),
      subaccountSid: this.normalizarTexto(this.formularioWhatsapp.subaccountSid),
      numeroRemitente: this.normalizarTexto(this.formularioWhatsapp.numeroRemitente),
      messagingServiceSid: this.normalizarTexto(this.formularioWhatsapp.messagingServiceSid),
      channelSenderSid: this.normalizarTexto(this.formularioWhatsapp.channelSenderSid),
      statusCallbackUrl: this.normalizarTexto(this.formularioWhatsapp.statusCallbackUrl),
      plantillaSolicitudConfirmacionSid: this.normalizarTexto(this.formularioWhatsapp.plantillaSolicitudConfirmacionSid),
      plantillaReprogramadaPendienteSid: this.normalizarTexto(this.formularioWhatsapp.plantillaReprogramadaPendienteSid),
      plantillaRecordatorioConfirmacionSid: this.normalizarTexto(this.formularioWhatsapp.plantillaRecordatorioConfirmacionSid),
      plantillaCitaConfirmadaSid: this.normalizarTexto(this.formularioWhatsapp.plantillaCitaConfirmadaSid),
      plantillaRecordatorioSid: this.normalizarTexto(this.formularioWhatsapp.plantillaRecordatorioSid),
      plantillaCancelacionSid: this.normalizarTexto(this.formularioWhatsapp.plantillaCancelacionSid),
      plantillaLiberadaSinConfirmacionSid: this.normalizarTexto(this.formularioWhatsapp.plantillaLiberadaSinConfirmacionSid),
      plantillaGraciasVisitaSid: this.normalizarTexto(this.formularioWhatsapp.plantillaGraciasVisitaSid),
      plantillaRecordatorioRegresoSid: this.normalizarTexto(this.formularioWhatsapp.plantillaRecordatorioRegresoSid),
      senderDisplayName: this.normalizarTexto(this.formularioWhatsapp.senderDisplayName),
      senderPhoneNumber: this.normalizarTexto(this.formularioWhatsapp.senderPhoneNumber),
      senderStatus: this.normalizarTexto(this.formularioWhatsapp.senderStatus),
      qualityRating: this.normalizarTexto(this.formularioWhatsapp.qualityRating),
      throughputMps: this.formularioWhatsapp.throughputMps ? Number(this.formularioWhatsapp.throughputMps) : null,
      wabaId: this.normalizarTexto(this.formularioWhatsapp.wabaId),
      metaBusinessManagerId: this.normalizarTexto(this.formularioWhatsapp.metaBusinessManagerId)
    };

    this.adminService.actualizarConfiguracionWhatsapp(payload)
      .pipe(finalize(() => this.guardandoWhatsapp = false))
      .subscribe({
        next: response => {
          this.configuracionWhatsapp.set(response);
          this.sincronizarFormularioWhatsapp(response);
          this.mensajeExito = 'La configuración de WhatsApp se guardó correctamente.';
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la configuración de WhatsApp.';
        }
      });
  }

  provisionarSubcuentaWhatsapp() {
    this.provisionandoSubcuentaWhatsapp = true;
    this.error = '';
    this.mensajeExito = '';

    const payload: ProvisionarSubcuentaWhatsappPayload = {
      friendlyName: this.normalizarTexto(this.formularioProvisionSubcuentaWhatsapp.friendlyName)
    };

    this.adminService.provisionarSubcuentaWhatsapp(payload)
      .pipe(finalize(() => this.provisionandoSubcuentaWhatsapp = false))
      .subscribe({
        next: response => {
          this.configuracionWhatsapp.set(response.configuracion);
          this.sincronizarFormularioWhatsapp(response.configuracion);
          this.mensajeExito = response.mensaje;
        },
        error: err => {
          this.error = err?.error?.message || err?.error?.mensaje || 'No se pudo provisionar la subcuenta de Twilio.';
        }
      });
  }

  provisionarMessagingServiceWhatsapp() {
    this.provisionandoMessagingServiceWhatsapp = true;
    this.error = '';
    this.mensajeExito = '';

    const payload: ProvisionarMessagingServiceWhatsappPayload = {
      friendlyName: this.normalizarTexto(this.formularioProvisionMessagingServiceWhatsapp.friendlyName),
      inboundRequestUrl: this.normalizarTexto(this.formularioProvisionMessagingServiceWhatsapp.inboundRequestUrl)
    };

    this.adminService.provisionarMessagingServiceWhatsapp(payload)
      .pipe(finalize(() => this.provisionandoMessagingServiceWhatsapp = false))
      .subscribe({
        next: (response: ProvisionarMessagingServiceWhatsappResponse) => {
          this.configuracionWhatsapp.set(response.configuracion);
          this.sincronizarFormularioWhatsapp(response.configuracion);
          this.mensajeExito = response.mensaje;
        },
        error: err => {
          this.error = err?.error?.message || err?.error?.mensaje || 'No se pudo provisionar el Messaging Service de Twilio.';
        }
      });
  }

  asociarChannelSenderWhatsapp() {
    this.asociandoChannelSenderWhatsapp = true;
    this.error = '';
    this.mensajeExito = '';

    const payload: AsociarChannelSenderWhatsappPayload = {
      channelSenderSid: this.formularioAsociacionChannelSenderWhatsapp.channelSenderSid.trim()
    };

    this.adminService.asociarChannelSenderWhatsapp(payload)
      .pipe(finalize(() => this.asociandoChannelSenderWhatsapp = false))
      .subscribe({
        next: (response: AsociarChannelSenderWhatsappResponse) => {
          this.configuracionWhatsapp.set(response.configuracion);
          this.sincronizarFormularioWhatsapp(response.configuracion);
          this.mensajeExito = response.mensaje;
        },
        error: err => {
          this.error = err?.error?.message || err?.error?.mensaje || 'No se pudo asociar el Channel Sender al Messaging Service.';
        }
      });
  }

  detectarChannelSenderWhatsapp() {
    this.detectandoChannelSenderWhatsapp = true;
    this.error = '';
    this.mensajeExito = '';

    this.adminService.detectarChannelSenderWhatsapp()
      .pipe(finalize(() => this.detectandoChannelSenderWhatsapp = false))
      .subscribe({
        next: (response: DetectarChannelSenderWhatsappResponse) => {
          this.configuracionWhatsapp.set(response.configuracion);
          this.sincronizarFormularioWhatsapp(response.configuracion);
          this.mensajeExito = response.mensaje;
        },
        error: err => {
          this.error = err?.error?.message || err?.error?.mensaje || 'No se pudo detectar el sender en Twilio.';
        }
      });
  }

  probarPlantillaWhatsapp() {
    this.probandoPlantillaWhatsapp = true;
    this.error = '';
    this.mensajeExito = '';
    const payload: ProbarPlantillaWhatsappPayload = {
      telefonoDestino: this.formularioPruebaWhatsapp.telefonoDestino.trim(),
      nombreCliente: this.formularioPruebaWhatsapp.nombreCliente.trim(),
      fecha: this.formularioPruebaWhatsapp.fecha.trim(),
      hora: this.formularioPruebaWhatsapp.hora.trim(),
      plantillaSid: this.normalizarTexto(this.formularioPruebaWhatsapp.plantillaSid)
    };

    this.adminService.probarPlantillaWhatsapp(payload)
      .pipe(finalize(() => this.probandoPlantillaWhatsapp = false))
      .subscribe({
        next: (response: PruebaWhatsappResponse) => {
          this.mensajeExito = response.mensaje || 'La prueba de plantilla fue enviada.';
          this.adminService.getLogsWhatsapp().subscribe({
            next: logs => this.logsWhatsapp.set(logs),
            error: () => undefined
          });
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo enviar la prueba de WhatsApp.';
        }
      });
  }

  editarSucursal(sucursal: SucursalAdmin) {
    this.sucursalEditandoId = sucursal.id;
    this.formularioSucursal = {
      nombre: sucursal.nombre,
      direccion: sucursal.direccion ?? '',
      telefono: sucursal.telefono ?? '',
      zonaHoraria: sucursal.zonaHoraria,
      activa: sucursal.activa
    };
  }

  cancelarEdicionSucursal() {
    this.sucursalEditandoId = null;
    this.formularioSucursal = {
      nombre: '',
      direccion: '',
      telefono: '',
      zonaHoraria: 'America/Mexico_City',
      activa: true
    };
  }

  guardarSucursal() {
    this.guardandoSucursal = true;
    this.error = '';
    this.mensajeExito = '';
    const payload: GuardarSucursalPayload = {
      ...this.formularioSucursal,
      direccion: this.normalizarTexto(this.formularioSucursal.direccion),
      telefono: this.normalizarTexto(this.formularioSucursal.telefono)
    };

    const operacion = this.sucursalEditandoId
      ? this.adminService.actualizarSucursal(this.sucursalEditandoId, payload)
      : this.adminService.crearSucursal(payload);

    operacion
      .pipe(finalize(() => this.guardandoSucursal = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionSucursal();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la sucursal.';
        }
      });
  }

  editarServicio(servicio: ServicioAdmin) {
    this.servicioEditandoId = servicio.id;
    this.formularioServicio = {
      sucursalId: servicio.sucursalId,
      nombre: servicio.nombre,
      descripcion: servicio.descripcion ?? '',
      duracionMinutos: servicio.duracionMinutos,
      bufferAntesMinutos: servicio.bufferAntesMinutos,
      bufferDespuesMinutos: servicio.bufferDespuesMinutos,
      precio: servicio.precio,
      moneda: servicio.moneda,
      activo: servicio.activo
    };
  }

  cancelarEdicionServicio() {
    this.servicioEditandoId = null;
    this.formularioServicio = {
      sucursalId: this.sucursales()[0]?.id ?? 0,
      nombre: '',
      descripcion: '',
      duracionMinutos: 60,
      bufferAntesMinutos: 0,
      bufferDespuesMinutos: 0,
      precio: 0,
      moneda: 'MXN',
      activo: true
    };
  }

  guardarServicio() {
    this.guardandoServicio = true;
    this.error = '';
    this.mensajeExito = '';
    const payload: GuardarServicioPayload = {
      ...this.formularioServicio,
      descripcion: this.normalizarTexto(this.formularioServicio.descripcion),
      moneda: (this.formularioServicio.moneda || 'MXN').toUpperCase()
    };

    const operacion = this.servicioEditandoId
      ? this.adminService.actualizarServicio(this.servicioEditandoId, payload)
      : this.adminService.crearServicio(payload);

    operacion
      .pipe(finalize(() => this.guardandoServicio = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionServicio();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar el servicio.';
        }
      });
  }

  editarUsuarioInterno(usuario: UsuarioInternoAdmin) {
    this.usuarioInternoEditandoId = usuario.usuarioId;
    this.formularioUsuarioInterno = {
      sucursalId: usuario.sucursalId,
      correo: usuario.correo,
      contrasenaTemporal: '',
      nombreCompleto: usuario.nombreCompleto,
      telefono: usuario.telefono ?? '',
      puesto: usuario.puesto ?? '',
      rolCodigo: usuario.rolCodigo,
      activo: usuario.activo,
      notas: usuario.notas ?? ''
    };
  }

  cancelarEdicionUsuarioInterno() {
    this.usuarioInternoEditandoId = null;
    this.formularioUsuarioInterno = {
      sucursalId: this.sucursales().length === 1 ? this.sucursales()[0].id : null,
      correo: '',
      contrasenaTemporal: '',
      nombreCompleto: '',
      telefono: '',
      puesto: '',
      rolCodigo: 'RECEPCIONISTA',
      activo: true,
      notas: ''
    };
  }

  guardarUsuarioInterno() {
    this.loading.set(true);
    this.error = '';
    this.mensajeExito = '';
    const contrasenaTemporal = this.normalizarTexto(this.formularioUsuarioInterno.contrasenaTemporal);
    const errorContrasena = this.validarContrasenaUsuarioInterno(contrasenaTemporal);
    if (errorContrasena) {
      this.error = errorContrasena;
      this.loading.set(false);
      return;
    }

    const payload: GuardarUsuarioInternoPayload = {
      ...this.formularioUsuarioInterno,
      sucursalId: this.formularioUsuarioInterno.sucursalId,
      correo: this.formularioUsuarioInterno.correo.trim().toLowerCase(),
      contrasenaTemporal,
      telefono: this.normalizarTexto(this.formularioUsuarioInterno.telefono),
      puesto: this.normalizarTexto(this.formularioUsuarioInterno.puesto),
      notas: this.normalizarTexto(this.formularioUsuarioInterno.notas),
      nombreCompleto: this.formularioUsuarioInterno.nombreCompleto.trim(),
      rolCodigo: this.formularioUsuarioInterno.rolCodigo
    };

    const operacion = this.usuarioInternoEditandoId
      ? this.adminService.actualizarUsuarioInterno(this.usuarioInternoEditandoId, payload)
      : this.adminService.crearUsuarioInterno(payload);

    operacion
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.cancelarEdicionUsuarioInterno();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar el usuario interno.';
        }
      });
  }

  editarPrestador(prestador: PrestadorAdmin) {
    this.prestadorEditandoId = prestador.usuarioId;
    this.formularioPrestador = {
      sucursalId: prestador.sucursalId,
      correo: prestador.correo,
      contrasenaTemporal: '',
      nombreMostrar: prestador.nombreMostrar,
      biografia: prestador.biografia ?? '',
      colorAgenda: prestador.colorAgenda ?? '#2563eb',
      activo: prestador.activo,
      servicioIds: [...prestador.servicioIds]
    };
    this.actualizarServiciosPrestadorDisponibles();
  }

  cancelarEdicionPrestador() {
    this.prestadorEditandoId = null;
    this.formularioPrestador = {
      sucursalId: this.sucursales()[0]?.id ?? 0,
      correo: '',
      contrasenaTemporal: '',
      nombreMostrar: '',
      biografia: '',
      colorAgenda: '#2563eb',
      activo: true,
      servicioIds: []
    };
    this.actualizarServiciosPrestadorDisponibles();
  }

  guardarPrestador() {
    this.guardandoPrestador = true;
    this.error = '';
    this.mensajeExito = '';
    const contrasenaTemporal = this.normalizarTexto(this.formularioPrestador.contrasenaTemporal);
    const errorContrasena = this.validarContrasenaPrestador(contrasenaTemporal);
    if (errorContrasena) {
      this.error = errorContrasena;
      this.guardandoPrestador = false;
      return;
    }

    const payload: GuardarPrestadorPayload = {
      ...this.formularioPrestador,
      correo: this.formularioPrestador.correo.trim().toLowerCase(),
      contrasenaTemporal,
      biografia: this.normalizarTexto(this.formularioPrestador.biografia),
      colorAgenda: this.normalizarTexto(this.formularioPrestador.colorAgenda)
    };

    const operacion = this.prestadorEditandoId
      ? this.adminService.actualizarPrestador(this.prestadorEditandoId, payload)
      : this.adminService.crearPrestador(payload);

    operacion
      .pipe(finalize(() => this.guardandoPrestador = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionPrestador();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar el prestador.';
        }
      });
  }

  cambiarSucursalPrestador(sucursalId: number) {
    this.formularioPrestador.sucursalId = sucursalId;
    this.actualizarServiciosPrestadorDisponibles();
    const serviciosValidos = new Set(this.serviciosPrestadorDisponibles.map(servicio => servicio.id));
    this.formularioPrestador.servicioIds = this.formularioPrestador.servicioIds.filter(id => serviciosValidos.has(id));
  }

  alternarServicioPrestador(servicioId: number, marcado: boolean) {
    const seleccionados = new Set(this.formularioPrestador.servicioIds);
    if (marcado) {
      seleccionados.add(servicioId);
    } else {
      seleccionados.delete(servicioId);
    }
    this.formularioPrestador.servicioIds = Array.from(seleccionados);
  }

  private construirOpcionesSujetos(tipoSujeto: string): Array<{ id: number; nombre: string }> {
    if (tipoSujeto === 'PRESTADOR') {
      return this.prestadores().map(prestador => ({
        id: prestador.usuarioId,
        nombre: `${prestador.nombreMostrar} · ${prestador.sucursalNombre}`
      }));
    }
    return this.sucursales().map(sucursal => ({
      id: sucursal.id,
      nombre: sucursal.nombre
    }));
  }

  cambiarTipoSujetoRegla(tipoSujeto: string) {
    this.formularioRegla.tipoSujeto = tipoSujeto;
    this.actualizarSujetosRegla();
  }

  cambiarTipoSujetoExcepcion(tipoSujeto: string) {
    this.formularioExcepcion.tipoSujeto = tipoSujeto;
    this.actualizarSujetosExcepcion();
  }

  editarRegla(regla: ReglaDisponibilidadAdmin) {
    this.reglaEditandoId = regla.id;
    this.formularioRegla = {
      tipoSujeto: regla.tipoSujeto,
      sujetoId: regla.sujetoId,
      diaSemana: regla.diaSemana,
      horaInicio: regla.horaInicio,
      horaFin: regla.horaFin,
      intervaloMinutos: regla.intervaloMinutos,
      vigenteDesde: regla.vigenteDesde,
      vigenteHasta: regla.vigenteHasta
    };
  }

  cancelarEdicionRegla() {
    this.reglaEditandoId = null;
    this.formularioRegla = {
      tipoSujeto: 'SUCURSAL',
      sujetoId: 0,
      diaSemana: 1,
      horaInicio: '09:00',
      horaFin: '18:00',
      intervaloMinutos: 15,
      vigenteDesde: null,
      vigenteHasta: null
    };
    this.actualizarSujetosRegla();
  }

  guardarRegla() {
    this.guardandoRegla = true;
    this.error = '';
    this.mensajeExito = '';
    const payload: GuardarReglaDisponibilidadPayload = {
      ...this.formularioRegla,
      vigenteDesde: this.normalizarTexto(this.formularioRegla.vigenteDesde),
      vigenteHasta: this.normalizarTexto(this.formularioRegla.vigenteHasta)
    };
    const operacion = this.reglaEditandoId
      ? this.adminService.actualizarReglaDisponibilidad(this.reglaEditandoId, payload)
      : this.adminService.crearReglaDisponibilidad(payload);

    operacion
      .pipe(finalize(() => this.guardandoRegla = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionRegla();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la regla de disponibilidad.';
        }
      });
  }

  editarExcepcion(excepcion: ExcepcionDisponibilidadAdmin) {
    this.excepcionEditandoId = excepcion.id;
    this.formularioExcepcion = {
      tipoSujeto: excepcion.tipoSujeto,
      sujetoId: excepcion.sujetoId,
      fechaExcepcion: excepcion.fechaExcepcion,
      horaInicio: excepcion.horaInicio,
      horaFin: excepcion.horaFin,
      tipoBloqueo: excepcion.tipoBloqueo,
      motivo: excepcion.motivo
    };
  }

  cancelarEdicionExcepcion() {
    this.excepcionEditandoId = null;
    this.formularioExcepcion = {
      tipoSujeto: 'SUCURSAL',
      sujetoId: 0,
      fechaExcepcion: '',
      horaInicio: null,
      horaFin: null,
      tipoBloqueo: 'BLOQUEO',
      motivo: null
    };
    this.actualizarSujetosExcepcion();
  }

  guardarExcepcion() {
    this.guardandoExcepcion = true;
    this.error = '';
    this.mensajeExito = '';
    const payload: GuardarExcepcionDisponibilidadPayload = {
      ...this.formularioExcepcion,
      horaInicio: this.normalizarTexto(this.formularioExcepcion.horaInicio),
      horaFin: this.normalizarTexto(this.formularioExcepcion.horaFin),
      motivo: this.normalizarTexto(this.formularioExcepcion.motivo)
    };
    const operacion = this.excepcionEditandoId
      ? this.adminService.actualizarExcepcionDisponibilidad(this.excepcionEditandoId, payload)
      : this.adminService.crearExcepcionDisponibilidad(payload);

    operacion
      .pipe(finalize(() => this.guardandoExcepcion = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionExcepcion();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la excepción de disponibilidad.';
        }
      });
  }

  private normalizarTexto(valor: string | null): string | null {
    const limpio = valor?.trim();
    return limpio ? limpio : null;
  }

  private construirAccionesAgendaAdmin(citaId: number, estado: string, whatsappUrl: string | null): AgendaActionVm[] {
    const acciones: AgendaActionVm[] = [];

    if (estado === 'PENDIENTE') {
      acciones.push({ id: 'confirm', label: 'Confirmar', kind: 'primary' });
      acciones.push({ id: 'cancel', label: 'Cancelar', kind: 'danger' });
    }

    if (estado === 'CONFIRMADA') {
      acciones.push({ id: 'finalize', label: 'Finalizar', kind: 'primary' });
      acciones.push({ id: 'no_show', label: 'No asistió', kind: 'secondary' });
      acciones.push({ id: 'cancel', label: 'Cancelar', kind: 'danger' });
    }

    acciones.push({ id: 'reschedule', label: 'Reprogramar', kind: 'ghost', disabled: true });

    if (whatsappUrl) {
      acciones.push({ id: 'whatsapp', label: 'Enviar WhatsApp', kind: 'secondary', externalUrl: whatsappUrl });
    }

    return acciones;
  }

  private actualizarEstadoAgenda(operacion: () => Observable<void>): void {
    this.loading.set(true);
    this.error = '';
    this.mensajeExito = '';
    operacion()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.mensajeExito = 'La cita se actualizó correctamente.';
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo actualizar la cita.';
        }
      });
  }

  private formatearMonedaAgenda(precio: number, moneda?: string | null): string {
    return new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: moneda || 'MXN',
      maximumFractionDigits: 0
    }).format(precio);
  }

  private validarContrasenaPrestador(contrasenaTemporal: string | null): string | null {
    if (!this.prestadorEditandoId && !contrasenaTemporal) {
      return 'La contraseña temporal es obligatoria y debe tener entre 8 y 100 caracteres.';
    }

    if (contrasenaTemporal && (contrasenaTemporal.length < 8 || contrasenaTemporal.length > 100)) {
      return 'La contraseña temporal debe tener entre 8 y 100 caracteres.';
    }

    return null;
  }

  private validarContrasenaUsuarioInterno(contrasenaTemporal: string | null): string | null {
    if (!this.usuarioInternoEditandoId && !contrasenaTemporal) {
      return 'La contraseña temporal es obligatoria para usuarios internos.';
    }

    if (contrasenaTemporal && (contrasenaTemporal.length < 8 || contrasenaTemporal.length > 100)) {
      return 'La contraseña temporal debe tener entre 8 y 100 caracteres.';
    }

    return null;
  }

  private marcarErrorCarga(err: any, mensajeFallback: string) {
    if (!this.error) {
      this.error = err?.error?.mensaje || err?.message || mensajeFallback;
    }
  }

  private sincronizarFormularioCorreo(configuracion: ConfiguracionCorreoAdmin | null) {
    this.formularioCorreo = {
      habilitado: configuracion?.habilitado ?? false,
      proveedor: configuracion?.proveedor ?? 'SMTP',
      remitente: configuracion?.remitente ?? '',
      nombreRemitente: configuracion?.nombreRemitente ?? '',
      responderA: configuracion?.responderA ?? '',
      smtpHost: configuracion?.smtpHost ?? '',
      smtpPort: configuracion?.smtpPort ?? 587,
      smtpUsername: configuracion?.smtpUsername ?? '',
      smtpPassword: '',
      smtpAuth: configuracion?.smtpAuth ?? true,
      smtpStartTls: configuracion?.smtpStartTls ?? true,
      graphTenantId: configuracion?.graphTenantId ?? '',
      graphClientId: configuracion?.graphClientId ?? '',
      graphClientSecret: '',
      graphUserId: configuracion?.graphUserId ?? '',
      graphCertificateThumbprint: configuracion?.graphCertificateThumbprint ?? '',
      graphPrivateKeyPem: ''
    };
  }

  private sincronizarFormularioWhatsapp(configuracion: ConfiguracionWhatsappAdmin | null) {
    this.formularioWhatsapp = {
      habilitado: configuracion?.habilitado ?? false,
      accountSid: configuracion?.accountSid ?? '',
      authToken: '',
      tipoCuentaTwilio: configuracion?.tipoCuentaTwilio ?? 'PLATAFORMA',
      subaccountSid: configuracion?.subaccountSid ?? '',
      numeroRemitente: configuracion?.numeroRemitente ?? '',
      messagingServiceSid: configuracion?.messagingServiceSid ?? '',
      channelSenderSid: configuracion?.channelSenderSid ?? '',
      statusCallbackUrl: configuracion?.statusCallbackUrl ?? '',
      plantillaSolicitudConfirmacionSid: configuracion?.plantillaSolicitudConfirmacionSid ?? '',
      plantillaReprogramadaPendienteSid: configuracion?.plantillaReprogramadaPendienteSid ?? '',
      plantillaRecordatorioConfirmacionSid: configuracion?.plantillaRecordatorioConfirmacionSid ?? '',
      plantillaCitaConfirmadaSid: configuracion?.plantillaCitaConfirmadaSid ?? '',
      plantillaRecordatorioSid: configuracion?.plantillaRecordatorioSid ?? '',
      plantillaCancelacionSid: configuracion?.plantillaCancelacionSid ?? '',
      plantillaLiberadaSinConfirmacionSid: configuracion?.plantillaLiberadaSinConfirmacionSid ?? '',
      plantillaGraciasVisitaSid: configuracion?.plantillaGraciasVisitaSid ?? '',
      plantillaRecordatorioRegresoSid: configuracion?.plantillaRecordatorioRegresoSid ?? '',
      senderDisplayName: configuracion?.senderDisplayName ?? '',
      senderPhoneNumber: configuracion?.senderPhoneNumber ?? '',
      senderStatus: configuracion?.senderStatus ?? '',
      qualityRating: configuracion?.qualityRating ?? '',
      throughputMps: configuracion?.throughputMps ?? null,
      wabaId: configuracion?.wabaId ?? '',
      metaBusinessManagerId: configuracion?.metaBusinessManagerId ?? ''
    };

    this.formularioPruebaWhatsapp = {
      telefonoDestino: this.formularioPruebaWhatsapp.telefonoDestino || '',
      nombreCliente: this.formularioPruebaWhatsapp.nombreCliente || '',
      fecha: this.formularioPruebaWhatsapp.fecha || '',
      hora: this.formularioPruebaWhatsapp.hora || '',
      plantillaSid: configuracion?.plantillaSolicitudConfirmacionSid ?? configuracion?.plantillaCitaConfirmadaSid ?? null
    };

    this.formularioProvisionSubcuentaWhatsapp = {
      friendlyName: this.formularioProvisionSubcuentaWhatsapp.friendlyName || ''
    };

    this.formularioProvisionMessagingServiceWhatsapp = {
      friendlyName: this.formularioProvisionMessagingServiceWhatsapp.friendlyName || '',
      inboundRequestUrl: this.formularioProvisionMessagingServiceWhatsapp.inboundRequestUrl || ''
    };

    this.formularioAsociacionChannelSenderWhatsapp = {
      channelSenderSid: configuracion?.channelSenderSid ?? (this.formularioAsociacionChannelSenderWhatsapp.channelSenderSid || '')
    };
  }

  private actualizarServiciosPrestadorDisponibles() {
    this.serviciosPrestadorDisponibles = this.servicios().filter(
      servicio => servicio.sucursalId === this.formularioPrestador.sucursalId
    );
  }

  private actualizarSujetosRegla() {
    this.sujetosRegla = this.construirOpcionesSujetos(this.formularioRegla.tipoSujeto);
    if (!this.sujetosRegla.some(sujeto => sujeto.id === this.formularioRegla.sujetoId)) {
      this.formularioRegla.sujetoId = this.sujetosRegla[0]?.id ?? 0;
    }
  }

  private actualizarSujetosExcepcion() {
    this.sujetosExcepcion = this.construirOpcionesSujetos(this.formularioExcepcion.tipoSujeto);
    if (!this.sujetosExcepcion.some(sujeto => sujeto.id === this.formularioExcepcion.sujetoId)) {
      this.formularioExcepcion.sujetoId = this.sujetosExcepcion[0]?.id ?? 0;
    }
  }

  private inicializarFechaAgenda() {
    if (!this.fechaAgendaSeleccionada()) {
      this.fechaAgendaSeleccionada.set(this.obtenerFechaAgendaInicial());
    }
  }

  private obtenerFechaAgendaInicial(): string {
    const fechas = this.fechasConCitasAgenda();
    const hoy = this.obtenerFechaLocalISO();
    if (!fechas.length) {
      return hoy;
    }

    if (fechas.includes(hoy)) {
      return hoy;
    }

    return fechas.find(fecha => fecha >= hoy) ?? fechas[0];
  }

  private obtenerFechaLocalISO(fecha = new Date()): string {
    const year = fecha.getFullYear();
    const month = `${fecha.getMonth() + 1}`.padStart(2, '0');
    const day = `${fecha.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private sumarDiasAgenda(fechaIso: string, dias: number): string {
    const fecha = new Date(`${fechaIso}T12:00:00`);
    fecha.setDate(fecha.getDate() + dias);
    return this.obtenerFechaLocalISO(fecha);
  }

  private formatearFechaAgendaLarga(fechaIso: string): string {
    const fecha = new Date(`${fechaIso}T12:00:00`);
    return fecha.toLocaleDateString('es-MX', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }
}
