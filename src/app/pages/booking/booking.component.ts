import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AppointmentService } from '../../services/appointment.service';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, DateSelectArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { finalize, timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, FullCalendarModule],
  template: `
    <section class="py-5 bg-pink-light min-vh-100">
      <div class="container py-5">
        <div class="row justify-content-center">
          <div class="col-lg-10">
            <div class="card shadow-lg border-0 rounded-4 overflow-hidden">
              <div class="row g-0">
                <!-- Lado Izquierdo: Calendario -->
                <div class="col-md-7 p-4 bg-white border-end">
                  <h2 class="h4 fw-bold text-primary-color mb-4">1. Elige una fecha</h2>
                  <full-calendar [options]="calendarOptions"></full-calendar>
                  <p class="text-muted small mt-3">
                    * Trabajamos de Lunes a Sábado. El horario disponible se muestra al seleccionar un día.
                  </p>
                </div>

                <!-- Lado Derecho: Horarios y Formulario -->
                <div class="col-md-5 p-4 p-md-5">
                  <div class="text-center mb-4">
                    <h1 class="h2 fw-bold text-primary-color">Agendar Cita</h1>
                    <p class="text-muted" *ngIf="selectedDate()">Para el día: <strong>{{ selectedDate() | date:'fullDate' }}</strong></p>
                  </div>

                  <div *ngIf="!selectedDate()" class="alert alert-info text-center">
                    Selecciona una fecha en el calendario para ver horarios disponibles.
                  </div>

                  <div *ngIf="selectedDate()">
                    <h5 class="fw-bold mb-3">2. Selecciona la hora</h5>
                    <div class="d-flex flex-wrap gap-2 mb-4">
                      <button *ngFor="let hour of availableHours()"
                              type="button"
                              class="btn btn-sm"
                              [class.btn-primary]="selectedHour() === hour"
                              [class.btn-outline-primary]="selectedHour() !== hour"
                              (click)="selectHour(hour)">
                        {{ hour }}
                      </button>
                      <div *ngIf="availableHours().length === 0 && !loadingSlots" class="text-danger small">
                        No hay horarios disponibles para este día.
                      </div>
                      <div *ngIf="loadingSlots" class="spinner-border spinner-border-sm text-primary"></div>
                    </div>

                    <form [formGroup]="bookingForm" (ngSubmit)="onSubmit()" class="needs-validation" *ngIf="selectedHour()">
                      <h5 class="fw-bold mb-3">3. Tus datos</h5>
                      <div class="mb-3">
                        <label class="form-label fw-semibold">Nombre Completo</label>
                        <input type="text" formControlName="name" class="form-control rounded-3" placeholder="Ej. Ana García" [class.is-invalid]="f['name'].touched && f['name'].invalid">
                      </div>

                      <div class="mb-3">
                        <label class="form-label fw-semibold">Teléfono</label>
                        <input type="tel" formControlName="phone" class="form-control rounded-3" placeholder="+521..." [class.is-invalid]="f['phone'].touched && f['phone'].invalid">

                      </div>

                      <div class="mb-3">
                        <label class="form-label fw-semibold">Correo</label>
                        <input type="email" formControlName="email" class="form-control rounded-3" placeholder="ana@mail.com" [class.is-invalid]="f['email'].touched && f['email'].invalid">
                      </div>

                      <div class="mb-3">
                        <label class="form-label fw-semibold">Servicio</label>
                        <select formControlName="service" class="form-select rounded-3" [class.is-invalid]="f['service'].touched && f['service'].invalid">
                          <option value="">Selecciona un servicio</option>
                          <option *ngFor="let s of services" [value]="s">{{ s }}</option>
                        </select>
                      </div>

                      <div class="d-grid mt-4">
                        <button type="submit" [disabled]="bookingForm.invalid || isSubmitting" class="btn btn-primary btn-lg rounded-pill fw-bold shadow-sm">
                          <span *ngIf="!isSubmitting">Confirmar Reserva para las {{ selectedHour() }}</span>
                          <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
                        </button>
                      </div>
                    </form>
                  </div>

                  <div class="mt-4 alert alert-success d-flex flex-column align-items-center" *ngIf="submitted || errorMessage">
                    <div class="d-flex align-items-center mb-2" *ngIf="submitted">
                      <span class="me-2">✅</span> <strong>¡Cita enviada con éxito!</strong>
                    </div>
                    <div class="text-center" [class.text-success]="submitted" [class.text-danger]="!submitted && errorMessage">
                      {{ errorMessage }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .bg-pink-light { background-color: #fff5f8; }
    .text-primary-color { color: #e91e63; }
    .btn-primary { background-color: #e91e63; border-color: #e91e63; }
    .btn-primary:hover { background-color: #c2185b; border-color: #c2185b; }
    .form-control:focus, .form-select:focus {
      border-color: #e91e63;
      box-shadow: 0 0 0 0.25rem rgba(233, 30, 99, 0.1);
    }
    ::ng-deep .fc .fc-button-primary {
      background-color: #e91e63;
      border-color: #e91e63;
    }
    ::ng-deep .fc .fc-button-primary:hover {
      background-color: #c2185b;
      border-color: #c2185b;
    }
    ::ng-deep .fc .fc-day-today {
      background-color: rgba(233, 30, 99, 0.05) !important;
    }
  `]
})
export class BookingComponent {
  bookingForm: FormGroup;
  submitted = false;
  isSubmitting = false;
  loadingSlots = false;
  selectedDate = signal<string | null>(null);
  selectedHour = signal<string | null>(null);
  availableHours = signal<string[]>([]);

  services = [
    'Manicura',
    'Pedicura',
    'Uñas Acrílicas',
    'Uñas de Gel',
    'Nail Art',
    'Retiro de Gel/Acrílico',
    'Diseño Especial'
  ];

  errorMessage = '';

  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    locale: 'es',
    selectable: true,
    weekends: true, // Se maneja en selectAllow
    headerToolbar: {
      left: 'prev,next',
      center: 'title',
      right: ''
    },
    selectAllow: (selectInfo) => {
      const today = new Date();
      today.setHours(0,0,0,0);
      const maxDate = new Date();
      maxDate.setDate(today.getDate() + 10);

      // No permitir domingos (0) ni fechas fuera de rango
      const day = selectInfo.start.getUTCDay();
      return selectInfo.start >= today && selectInfo.start <= maxDate && day !== 0;
    },
    select: (arg: DateSelectArg) => {
      const dateStr = arg.startStr;
      this.onDateSelect(dateStr);
    }
  };

  constructor(private fb: FormBuilder, private appointmentService: AppointmentService) {
    this.bookingForm = this.fb.group({
      name: ['', Validators.required],
      phone: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      service: ['', Validators.required]
    });
  }

  get f() { return this.bookingForm.controls; }

  onDateSelect(date: string) {
    this.selectedDate.set(date);
    this.selectedHour.set(null);
    this.loadingSlots = true;
    this.errorMessage = '';

    this.appointmentService.getOccupiedSlots(date).subscribe({
      next: (response) => {
        let occupied: string[] = [];
        try {
          occupied = JSON.parse(response);
        } catch (e) {
          console.warn('No se pudieron obtener horarios ocupados, usando todos como disponibles.');
        }
        this.generateAvailableHours(occupied);
        this.loadingSlots = false;
      },
      error: () => {
        console.warn('Error al conectar con el script para horarios, usando todos como disponibles.');
        this.generateAvailableHours([]);
        this.loadingSlots = false;
      }
    });
  }

  generateAvailableHours(occupied: string[]) {
    const hours: string[] = [];
    for (let h = 8; h <= 21; h++) { // Horario flexible según el script
      const hourStr = `${h.toString().padStart(2, '0')}:00`;
      if (!occupied.includes(hourStr)) {
        hours.push(hourStr);
      }
    }
    this.availableHours.set(hours);
  }

  selectHour(hour: string) {
    this.selectedHour.set(hour);
  }

  onSubmit() {
    if (this.bookingForm.valid && this.selectedDate() && this.selectedHour()) {
      this.isSubmitting = true;
      this.submitted = false;
      this.errorMessage = '';

      let phone = this.bookingForm.value.phone.trim();
      // Si no empieza con '+', y no empieza con '521', lo agregamos.
      if (!phone.startsWith('+')) {
        if (phone.startsWith('521')) {
          phone = '+' + phone;
        } else {
          phone = '+521' + phone;
        }
      }

      const formData = {
        token: 'AGENDA2025', // Aseguramos que el token vaya al principio como en el ejemplo de Postman
        nombre: this.bookingForm.value.name.trim(),
        telefono: phone,
        correo: this.bookingForm.value.email.trim(),
        servicio: this.bookingForm.value.service,
        fecha: this.selectedDate(),
        hora: this.selectedHour()
      };

      console.log('Enviando datos de reserva:', formData);

      this.appointmentService.bookAppointment(formData)
        .pipe(
          timeout(15000),
          finalize(() => this.isSubmitting = false),
          catchError(error => {
            console.error('Error al agendar cita:', error);
            // En Google Apps Script, un error de CORS a menudo significa que la petición se envió
            // pero el navegador bloqueó la lectura de la respuesta de redirección.
            this.handleSuccess(true);
            return of(null);
          })
        )
        .subscribe({
          next: (response: any) => {
            if (!response) return;

            let resData;
            try {
              resData = typeof response === 'string' ? JSON.parse(response) : response;
            } catch (e) {
              this.handleSuccess();
              return;
            }

            if (resData && resData.success) {
              this.handleSuccess();
            } else {
              this.errorMessage = resData?.message || 'Hubo un problema al procesar la cita.';
            }
          }
        });
    }
  }

  private handleSuccess(isMaybe = false) {
    this.submitted = true;
    this.isSubmitting = false;
    this.bookingForm.reset();
    this.selectedDate.set(null);
    this.selectedHour.set(null);
    if (isMaybe) {
      this.errorMessage = 'Nota: La respuesta del servidor fue inusual, pero tu cita probablemente fue enviada. Revisa tu correo y confirma por WhatsApp al +1 267 313 6057.';
    } else {
      this.errorMessage = '¡Cita enviada! Revisa tu correo y recuerda confirmar por WhatsApp escribiendo "Hola" al +1 267 313 6057.';
    }
    setTimeout(() => {
      this.submitted = false;
      this.errorMessage = '';
    }, 10000);
  }
}
