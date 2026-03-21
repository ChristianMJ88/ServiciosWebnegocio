import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import {
  CitaStaff,
  ExcepcionDisponibilidadStaff,
  GuardarExcepcionDisponibilidadStaffPayload,
  GuardarReglaDisponibilidadStaffPayload,
  ReglaDisponibilidadStaff,
  StaffService
} from '../../core/staff/staff.service';

type SeccionStaff = 'agenda' | 'horarios' | 'bloqueos';

@Component({
  selector: 'app-staff-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    MatBadgeModule,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatListModule,
    MatMenuModule,
    MatProgressBarModule,
    MatSelectModule,
    MatToolbarModule,
    MatTooltipModule
  ],
  templateUrl: './staff-dashboard.component.html',
  styleUrls: ['./staff-dashboard.component.css']
})
export class StaffDashboardComponent implements OnInit {
  private readonly staffService = inject(StaffService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly ngZone = inject(NgZone);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly inicioAgendaHora = 8;
  private readonly finAgendaHora = 20;
  private readonly alturaHoraAgenda = 84;

  readonly loading = signal(false);
  readonly fechaAgendaSeleccionada = signal<string | null>(null);
  readonly rangoAgendaDesde = signal<string | null>(null);
  readonly rangoAgendaHasta = signal<string | null>(null);
  readonly citaAgendaSeleccionadaId = signal<number | null>(null);
  readonly panelMovil = signal(false);
  readonly sidebarAbierto = signal(true);
  readonly sidebarCompacto = signal(false);
  readonly seccionActiva = signal<SeccionStaff>('agenda');
  readonly agenda = signal<CitaStaff[]>([]);
  readonly reglas = signal<ReglaDisponibilidadStaff[]>([]);
  readonly excepciones = signal<ExcepcionDisponibilidadStaff[]>([]);
  readonly diasSemana = [
    { value: 1, label: 'Lunes' },
    { value: 2, label: 'Martes' },
    { value: 3, label: 'Miércoles' },
    { value: 4, label: 'Jueves' },
    { value: 5, label: 'Viernes' },
    { value: 6, label: 'Sábado' },
    { value: 7, label: 'Domingo' }
  ];
  readonly tiposBloqueo = ['BLOQUEO', 'DESCANSO', 'VACACIONES', 'HORARIO_ESPECIAL'];
  readonly modulosStaff: Array<{ id: SeccionStaff; titulo: string; descripcion: string; icono: string }> = [
    { id: 'agenda', titulo: 'Agenda', descripcion: 'Citas del día y acciones operativas.', icono: 'agenda' },
    { id: 'horarios', titulo: 'Horarios', descripcion: 'Define tu disponibilidad base.', icono: 'horario' },
    { id: 'bloqueos', titulo: 'Bloqueos', descripcion: 'Descansos, vacaciones y cierres.', icono: 'bloqueo' }
  ];
  readonly moduloActivo = computed(() => this.modulosStaff.find(modulo => modulo.id === this.seccionActiva()) ?? this.modulosStaff[0]);
  readonly nombreUsuario = computed(() => this.authService.nombreUsuarioVisible());
  readonly correoUsuario = computed(() => this.authService.sesionActual()?.correo ?? '');
  readonly inicialesUsuario = computed(() => this.authService.inicialesUsuarioVisible());
  readonly notificaciones = computed(() => this.agenda().filter(cita => cita.estado === 'PENDIENTE').length);
  readonly horasAgenda = Array.from({ length: this.finAgendaHora - this.inicioAgendaHora + 1 }, (_, index) => {
    const hora = this.inicioAgendaHora + index;
    return { hora, etiqueta: `${hora.toString().padStart(2, '0')}:00` };
  });
  readonly fechasConCitasAgenda = computed(() =>
    Array.from(new Set(this.agenda().map(cita => cita.inicio.slice(0, 10)))).sort((a, b) => a.localeCompare(b))
  );
  readonly agendaFechaActiva = computed(() => {
    return this.fechaAgendaSeleccionada() ?? this.obtenerFechaAgendaInicial();
  });
  readonly agendaFechaActivaTexto = computed(() => this.formatearFechaAgendaLarga(this.agendaFechaActiva()));
  readonly citasAgenda = computed(() => {
    const fecha = this.agendaFechaActiva();
    const citasDelDia = this.agenda()
      .filter(cita => cita.inicio.startsWith(fecha))
      .sort((a, b) => new Date(a.inicio).getTime() - new Date(b.inicio).getTime());
    const distribucion = this.calcularDistribucionAgenda(citasDelDia);

    return citasDelDia.map(cita => {
        const inicio = new Date(cita.inicio);
        const fin = new Date(cita.fin);
        const minutosInicio = (inicio.getHours() - this.inicioAgendaHora) * 60 + inicio.getMinutes();
        const duracionMinutos = Math.max(30, Math.round((fin.getTime() - inicio.getTime()) / 60000));
        const posicion = distribucion.get(cita) ?? { lane: 0, laneCount: 1 };

        return {
          ...cita,
          top: (minutosInicio / 60) * this.alturaHoraAgenda,
          height: Math.max(66, (duracionMinutos / 60) * this.alturaHoraAgenda - 8),
          leftPct: (100 / posicion.laneCount) * posicion.lane,
          widthPct: 100 / posicion.laneCount
        };
      });
  });
  readonly resumenAgendaActiva = computed(() => {
    const citas = this.agenda().filter(cita => cita.inicio.startsWith(this.agendaFechaActiva()));
    return {
      total: citas.length,
      pendientes: citas.filter(cita => cita.estado === 'PENDIENTE').length,
      confirmadas: citas.filter(cita => cita.estado === 'CONFIRMADA').length,
      finalizadas: citas.filter(cita => cita.estado === 'FINALIZADA').length
    };
  });
  readonly citaAgendaSeleccionada = computed(() => {
    const citas = this.citasAgenda();
    if (!citas.length) {
      return null;
    }

    return citas.find(cita => cita.id === this.citaAgendaSeleccionadaId()) ?? citas[0];
  });
  readonly totalAgenda = computed(() => this.agenda().length);
  readonly totalConfirmadas = computed(() => this.agenda().filter(cita => cita.estado === 'CONFIRMADA').length);
  readonly totalFinalizadas = computed(() => this.agenda().filter(cita => cita.estado === 'FINALIZADA').length);

  error = '';
  guardandoRegla = false;
  guardandoExcepcion = false;
  reglaEditandoId: number | null = null;
  excepcionEditandoId: number | null = null;

  formularioRegla: GuardarReglaDisponibilidadStaffPayload = {
    tipoSujeto: 'PRESTADOR',
    sujetoId: 0,
    diaSemana: 1,
    horaInicio: '09:00',
    horaFin: '18:00',
    intervaloMinutos: 15,
    vigenteDesde: null,
    vigenteHasta: null
  };

  formularioExcepcion: GuardarExcepcionDisponibilidadStaffPayload = {
    tipoSujeto: 'PRESTADOR',
    sujetoId: 0,
    fechaExcepcion: '',
    horaInicio: null,
    horaFin: null,
    tipoBloqueo: 'BLOQUEO',
    motivo: null
  };

  ngOnInit(): void {
    this.actualizarVistaEnZona(() => {
      this.sincronizarSidebarConViewport(this.breakpointObserver.isMatched('(max-width: 991px)'));
    });
    this.recargar();

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

  seleccionarSeccion(seccion: SeccionStaff) {
    this.seccionActiva.set(seccion);
    if (this.panelMovil()) {
      this.sidebarAbierto.set(false);
    }
  }

  alternarCompactoSidebar() {
    if (this.panelMovil()) {
      this.sidebarAbierto.update(valor => !valor);
      return;
    }
    this.sidebarCompacto.update(valor => !valor);
  }

  cerrarSidebar() {
    this.sidebarAbierto.set(false);
  }

  tituloSeccionActual(): string {
    return this.modulosStaff.find(modulo => modulo.id === this.seccionActiva())?.titulo ?? 'Panel Staff';
  }

  descripcionSeccionActual(): string {
    return this.modulosStaff.find(modulo => modulo.id === this.seccionActiva())?.descripcion ?? '';
  }

  claseEstadoCita(estado: string): string {
    return `estado-${estado.toLowerCase()}`;
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
    const fechaObjetivo = fecha || this.obtenerFechaAgendaInicial();
    this.fechaAgendaSeleccionada.set(fechaObjetivo);
    this.citaAgendaSeleccionadaId.set(null);
    this.asegurarAgendaParaFecha(fechaObjetivo);
  }

  desplazarFechaAgenda(dias: number) {
    const fechaObjetivo = this.sumarDiasAgenda(this.agendaFechaActiva(), dias);
    this.fechaAgendaSeleccionada.set(fechaObjetivo);
    this.citaAgendaSeleccionadaId.set(null);
    this.asegurarAgendaParaFecha(fechaObjetivo);
  }

  irAHoyAgenda() {
    const hoy = this.obtenerFechaLocalISO();
    this.fechaAgendaSeleccionada.set(hoy);
    this.citaAgendaSeleccionadaId.set(null);
    this.asegurarAgendaParaFecha(hoy);
  }

  seleccionarCitaAgenda(citaId: number) {
    this.citaAgendaSeleccionadaId.set(citaId);
  }

  esCitaSeleccionada(citaId: number): boolean {
    return this.citaAgendaSeleccionada()?.id === citaId;
  }

  logout() {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }

  confirmar(id: number) {
    this.actualizarEstado(() => this.staffService.confirmar(id));
  }

  finalizar(id: number) {
    this.actualizarEstado(() => this.staffService.finalizar(id));
  }

  noAsistio(id: number) {
    this.actualizarEstado(() => this.staffService.noAsistio(id));
  }

  recargar() {
    const { desde, hasta } = this.obtenerRangoAgenda(this.fechaAgendaSeleccionada() ?? this.obtenerFechaAgendaInicial());
    this.actualizarVistaEnZona(() => {
      this.loading.set(true);
      this.error = '';
    });
    forkJoin({
      agenda: this.staffService.getAgenda(desde, hasta),
      reglas: this.staffService.getReglasDisponibilidad(),
      excepciones: this.staffService.getExcepcionesDisponibilidad()
    })
      .pipe(finalize(() => this.actualizarVistaEnZona(() => this.loading.set(false))))
      .subscribe({
        next: ({ agenda, reglas, excepciones }) => {
          this.actualizarVistaEnZona(() => {
            this.agenda.set(agenda);
            this.reglas.set(reglas);
            this.excepciones.set(excepciones);
            this.rangoAgendaDesde.set(desde);
            this.rangoAgendaHasta.set(hasta);
            this.citaAgendaSeleccionadaId.set(null);
            this.inicializarFechaAgenda();
          });
        },
        error: err => {
          this.actualizarVistaEnZona(() => {
            this.error = err?.error?.mensaje || err?.message || 'No se pudo cargar la agenda del staff.';
          });
        }
      });
  }

  editarRegla(regla: ReglaDisponibilidadStaff) {
    this.reglaEditandoId = regla.id;
    this.formularioRegla = {
      tipoSujeto: 'PRESTADOR',
      sujetoId: 0,
      diaSemana: regla.diaSemana,
      horaInicio: regla.horaInicio,
      horaFin: regla.horaFin,
      intervaloMinutos: regla.intervaloMinutos,
      vigenteDesde: regla.vigenteDesde,
      vigenteHasta: regla.vigenteHasta
    };
    this.seccionActiva.set('horarios');
  }

  cancelarEdicionRegla() {
    this.reglaEditandoId = null;
    this.formularioRegla = {
      tipoSujeto: 'PRESTADOR',
      sujetoId: 0,
      diaSemana: 1,
      horaInicio: '09:00',
      horaFin: '18:00',
      intervaloMinutos: 15,
      vigenteDesde: null,
      vigenteHasta: null
    };
  }

  guardarRegla() {
    this.guardandoRegla = true;
    this.error = '';
    const payload: GuardarReglaDisponibilidadStaffPayload = {
      ...this.formularioRegla,
      vigenteDesde: this.normalizarTexto(this.formularioRegla.vigenteDesde),
      vigenteHasta: this.normalizarTexto(this.formularioRegla.vigenteHasta)
    };

    const operacion = this.reglaEditandoId
      ? this.staffService.actualizarReglaDisponibilidad(this.reglaEditandoId, payload)
      : this.staffService.crearReglaDisponibilidad(payload);

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

  editarExcepcion(excepcion: ExcepcionDisponibilidadStaff) {
    this.excepcionEditandoId = excepcion.id;
    this.formularioExcepcion = {
      tipoSujeto: 'PRESTADOR',
      sujetoId: 0,
      fechaExcepcion: excepcion.fechaExcepcion,
      horaInicio: excepcion.horaInicio,
      horaFin: excepcion.horaFin,
      tipoBloqueo: excepcion.tipoBloqueo,
      motivo: excepcion.motivo
    };
    this.seccionActiva.set('bloqueos');
  }

  cancelarEdicionExcepcion() {
    this.excepcionEditandoId = null;
    this.formularioExcepcion = {
      tipoSujeto: 'PRESTADOR',
      sujetoId: 0,
      fechaExcepcion: '',
      horaInicio: null,
      horaFin: null,
      tipoBloqueo: 'BLOQUEO',
      motivo: null
    };
  }

  guardarExcepcion() {
    this.guardandoExcepcion = true;
    this.error = '';
    const payload: GuardarExcepcionDisponibilidadStaffPayload = {
      ...this.formularioExcepcion,
      horaInicio: this.normalizarTexto(this.formularioExcepcion.horaInicio),
      horaFin: this.normalizarTexto(this.formularioExcepcion.horaFin),
      motivo: this.normalizarTexto(this.formularioExcepcion.motivo)
    };

    const operacion = this.excepcionEditandoId
      ? this.staffService.actualizarExcepcionDisponibilidad(this.excepcionEditandoId, payload)
      : this.staffService.crearExcepcionDisponibilidad(payload);

    operacion
      .pipe(finalize(() => this.guardandoExcepcion = false))
      .subscribe({
        next: () => {
          this.cancelarEdicionExcepcion();
          this.recargar();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo guardar la excepción.';
        }
      });
  }

  etiquetaDiaSemana(dia: number): string {
    return this.diasSemana.find(item => item.value === dia)?.label ?? `Día ${dia}`;
  }

  private actualizarEstado(operation: () => any) {
    this.loading.set(true);
    this.error = '';
    operation()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => this.recargar(),
        error: (err: any) => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo actualizar el estado de la cita.';
        }
      });
  }

  private normalizarTexto(valor: string | null): string | null {
    const limpio = valor?.trim();
    return limpio ? limpio : null;
  }

  private asegurarAgendaParaFecha(fecha: string) {
    const desde = this.rangoAgendaDesde();
    const hasta = this.rangoAgendaHasta();

    if (!desde || !hasta || fecha < desde || fecha > hasta) {
      this.cargarAgendaParaFecha(fecha);
    }
  }

  private cargarAgendaParaFecha(fecha: string) {
    const { desde, hasta } = this.obtenerRangoAgenda(fecha);
    this.loading.set(true);
    this.error = '';
    this.staffService.getAgenda(desde, hasta)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: agenda => {
          this.agenda.set(agenda);
          this.rangoAgendaDesde.set(desde);
          this.rangoAgendaHasta.set(hasta);
          this.citaAgendaSeleccionadaId.set(null);
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo cargar la agenda para la fecha seleccionada.';
        }
      });
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

  private obtenerRangoAgenda(fechaIso: string) {
    const base = new Date(`${fechaIso}T12:00:00`);
    const inicio = new Date(base.getFullYear(), base.getMonth(), 1, 12, 0, 0);
    const fin = new Date(base.getFullYear(), base.getMonth() + 1, 0, 12, 0, 0);
    return {
      desde: this.obtenerFechaLocalISO(inicio),
      hasta: this.obtenerFechaLocalISO(fin)
    };
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
