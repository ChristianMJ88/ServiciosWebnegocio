import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
  CitaStaff,
  ExcepcionDisponibilidadStaff,
  GuardarExcepcionDisponibilidadStaffPayload,
  GuardarReglaDisponibilidadStaffPayload,
  ReglaDisponibilidadStaff,
  StaffService
} from '../../core/staff/staff.service';

@Component({
  selector: 'app-staff-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './staff-dashboard.component.html',
  styleUrl: './staff-dashboard.component.css'
})
export class StaffDashboardComponent implements OnInit {
  private readonly staffService = inject(StaffService);
  readonly loading = signal(false);
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
    this.recargar();
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
    this.loading.set(true);
    this.error = '';
    forkJoin({
      agenda: this.staffService.getAgenda(),
      reglas: this.staffService.getReglasDisponibilidad(),
      excepciones: this.staffService.getExcepcionesDisponibilidad()
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ agenda, reglas, excepciones }) => {
          this.agenda.set(agenda);
          this.reglas.set(reglas);
          this.excepciones.set(excepciones);
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo cargar la agenda del staff.';
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
}
