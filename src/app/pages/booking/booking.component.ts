import { Component, signal, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AppointmentService } from '../../services/appointment.service';
import { Router, RouterLink } from '@angular/router';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, DateSelectArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { finalize, timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    FullCalendarModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatStepperModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './booking.component.html',
  styleUrls: ['./booking.component.css']
})
export class BookingComponent implements OnInit {
  bookingForm: FormGroup;
  submitted = false;
  isSubmitting = false;
  loadingSlots = false;
  showConfirmation = signal<boolean>(false);
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
  isMobile = false;
  isTablet = false;

  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    this.checkScreenSize();
  }

  ngOnInit() {
    this.checkScreenSize();
  }

  private checkScreenSize() {
    this.isMobile = window.innerWidth < 768;
    this.isTablet = window.innerWidth >= 768 && window.innerWidth < 1024;
  }

  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    locale: 'es',
    selectable: true,
    unselectAuto: false,
    longPressDelay: 0, // Permite selección inmediata en táctiles
    headerToolbar: {
      left: 'prev,next',
      center: 'title',
      right: ''
    },
    contentHeight: 'auto',
    aspectRatio: 1.35,
    handleWindowResize: true,
    selectAllow: (selectInfo) => {
      const today = new Date();
      today.setHours(0,0,0,0);
      const maxDate = new Date();
      maxDate.setDate(today.getDate() + 30); // Aumentado a 30 días para mejor UX

      // No permitir domingos (0) ni fechas pasadas
      const day = selectInfo.start.getDay();
      const startDate = new Date(selectInfo.start);
      startDate.setHours(0,0,0,0);

      return startDate >= today && startDate <= maxDate && day !== 0;
    },
    select: (arg: DateSelectArg) => {
      const dateStr = arg.startStr;
      this.onDateSelect(dateStr);
    }
  };

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
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
    const now = new Date();
    const isToday = this.selectedDate() === now.toISOString().split('T')[0];

    for (let h = 8; h <= 20; h++) {
      const hourStr = `${h.toString().padStart(2, '0')}:00`;

      // Si es hoy, no mostrar horas que ya pasaron
      if (isToday) {
        const [hour] = hourStr.split(':').map(Number);
        if (hour <= now.getHours()) continue;
      }

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
    this.showConfirmation.set(true);
    this.bookingForm.reset();
    this.selectedDate.set(null);
    this.selectedHour.set(null);

    const message = isMaybe
      ? 'Nota: La respuesta del servidor fue inusual, pero tu cita probablemente fue enviada. Revisa tu correo.'
      : '¡Cita enviada con éxito!';

    this.snackBar.open(message, 'Cerrar', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
      panelClass: isMaybe ? ['warning-snackbar'] : ['success-snackbar']
    });
  }

  resetBooking() {
    this.showConfirmation.set(false);
    this.submitted = false;
    this.bookingForm.reset();
    this.selectedDate.set(null);
    this.selectedHour.set(null);
  }
}
