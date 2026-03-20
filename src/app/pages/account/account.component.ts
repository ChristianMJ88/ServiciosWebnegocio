import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ClientAppointmentsService, CitaCliente } from '../../core/auth/client-appointments.service';
import { finalize } from 'rxjs/operators';
import { AppointmentService, FranjaDisponible } from '../../services/appointment.service';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, DatePipe, CurrencyPipe],
  templateUrl: './account.component.html',
  styleUrl: './account.component.css'
})
export class AccountComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly citasService = inject(ClientAppointmentsService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly router = inject(Router);
  readonly loading = signal(false);
  readonly citas = signal<CitaCliente[]>([]);
  readonly citaEnReprogramacionId = signal<number | null>(null);
  readonly fechaReprogramacion = signal('');
  readonly franjasReprogramacion = signal<FranjaDisponible[]>([]);
  readonly franjaSeleccionada = signal<string | null>(null);
  readonly loadingFranjas = signal(false);
  error = '';

  ngOnInit(): void {
    this.loadAppointments();
  }

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }

  cancelAppointment(citaId: number) {
    this.loading.set(true);
    this.error = '';
    this.citasService.cancelAppointment(citaId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => this.loadAppointments(),
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo cancelar la cita.';
        }
      });
  }

  abrirReprogramacion(cita: CitaCliente) {
    this.citaEnReprogramacionId.set(cita.id);
    this.fechaReprogramacion.set('');
    this.franjasReprogramacion.set([]);
    this.franjaSeleccionada.set(null);
    this.error = '';
  }

  cerrarReprogramacion() {
    this.citaEnReprogramacionId.set(null);
    this.fechaReprogramacion.set('');
    this.franjasReprogramacion.set([]);
    this.franjaSeleccionada.set(null);
  }

  onFechaReprogramacionChange(cita: CitaCliente, fecha: string) {
    this.fechaReprogramacion.set(fecha);
    this.franjasReprogramacion.set([]);
    this.franjaSeleccionada.set(null);
    this.error = '';

    if (!fecha) {
      return;
    }

    this.loadingFranjas.set(true);
    this.appointmentService.getAvailableSlots({
      empresaId: this.authService.sesionActual()?.empresaId || 1,
      sucursalId: cita.sucursalId,
      servicioId: cita.servicioId,
      prestadorId: cita.prestadorId,
      fecha
    })
      .pipe(finalize(() => this.loadingFranjas.set(false)))
      .subscribe({
        next: franjas => this.franjasReprogramacion.set(franjas),
        error: err => {
          this.error = err?.message || 'No se pudieron cargar las franjas para reprogramar.';
        }
      });
  }

  seleccionarFranjaReprogramacion(franja: FranjaDisponible) {
    this.franjaSeleccionada.set(franja.inicio);
  }

  confirmarReprogramacion(citaId: number) {
    const nuevoInicio = this.franjaSeleccionada();
    if (!nuevoInicio) {
      this.error = 'Selecciona una nueva franja para reprogramar.';
      return;
    }

    this.loading.set(true);
    this.error = '';
    this.citasService.rescheduleAppointment(citaId, nuevoInicio)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.cerrarReprogramacion();
          this.loadAppointments();
        },
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudo reprogramar la cita.';
        }
      });
  }

  private loadAppointments() {
    this.loading.set(true);
    this.error = '';
    this.citasService.getMyAppointments()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: citas => this.citas.set(citas),
        error: err => {
          this.error = err?.error?.mensaje || err?.message || 'No se pudieron cargar tus citas.';
        }
      });
  }
}
