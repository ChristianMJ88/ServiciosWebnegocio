import { Component, signal, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AppointmentService } from '../../services/appointment.service';
import { BookingDataService } from '../../services/booking-data.service';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, DateSelectArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, FullCalendarModule],
  templateUrl: './booking.component.html',
  styleUrls: ['./booking.component.css']
})
export class BookingComponent {
  private fb = inject(FormBuilder);
  private appointmentService = inject(AppointmentService);
  private bookingDataService = inject(BookingDataService);

  @ViewChild('hoursSection') hoursSection!: ElementRef;
  @ViewChild('dataSection') dataSection!: ElementRef;
  @ViewChild('confirmationSection') confirmationSection!: ElementRef;

  bookingForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    phone: ['', [Validators.required, Validators.pattern('^[0-9+ ]{10,15}$')]],
    email: ['', [Validators.required, Validators.email]],
    service: ['', Validators.required]
  });

  submitted = signal(false);
  isSubmitting = signal(false);
  loadingSlots = signal(false);
  selectedDate = signal<string | null>(null);
  selectedHour = signal<string | null>(null);
  availableHours = signal<string[]>([]);
  errorMessage = '';

  services = this.bookingDataService.getServices();

  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    locale: 'es',
    selectable: true,
    unselectAuto: false,
    longPressDelay: 0,
    weekends: true,
    contentHeight: 'auto',
    fixedWeekCount: false,
    handleWindowResize: true,
    headerToolbar: {
      left: 'prev,next',
      center: 'title',
      right: ''
    },

    selectAllow: (selectInfo) => {
      const today = new Date();
      today.setHours(0,0,0,0);
      const maxDate = new Date();
      maxDate.setDate(today.getDate() + 30);

      const day = selectInfo.start.getDay(); // Usar getDay() local
      return selectInfo.start >= today && selectInfo.start <= maxDate && day !== 0;
    },
    select: (arg: DateSelectArg) => {
      this.onDateSelect(arg.startStr);
    }
  };

  get f() { return this.bookingForm.controls; }

  onDateSelect(date: string) {
    this.selectedDate.set(date);
    this.selectedHour.set(null);
    this.loadingSlots.set(true);
    this.errorMessage = '';
    this.availableHours.set([]);
    console.log('Solicitando horarios para:', date, 'loadingSlots:', this.loadingSlots());

    this.appointmentService.getAvailableSlots(date)
      .pipe(
        finalize(() => {
          this.loadingSlots.set(false);
          console.log('Finalize: loadingSlots set to false. Current value:', this.loadingSlots());
          setTimeout(() => this.scrollToHours(), 100);
        })
      )
      .subscribe({
        next: (response) => {
          console.log('1. Respuesta recibida del servidor');
          let available: string[] = [];
          try {
            const cleanedResponse = typeof response === 'string' ? response.trim() : response;
            console.log('2. Respuesta cruda:', cleanedResponse);

            if (cleanedResponse && cleanedResponse !== '[]') {
              if (typeof cleanedResponse === 'string' && cleanedResponse.startsWith('[')) {
                available = JSON.parse(cleanedResponse);
              } else if (Array.isArray(cleanedResponse)) {
                available = cleanedResponse;
              }
            }
          } catch (e) {
            console.warn('3. Error parseando JSON:', e);
          }
          console.log('4. Llamando a generateAvailableHours con:', available);
          this.generateAvailableHours(available);
        },
        error: (err) => {
          console.error('ERROR en la petición:', err);
          this.errorMessage = 'Error de conexión. Cargando horarios base.';
          this.generateAvailableHours([]);
        }
      });
  }

  private generateAvailableHours(backendAvailable: string[]) {
    console.log('Procesando horarios. Backend devolvió:', backendAvailable);

    // Si el backend nos dio una lista con elementos, la usamos.
    // Si viene vacío o nulo, usamos las horas de trabajo por defecto (fallback).
    const hasBackendData = Array.isArray(backendAvailable) && backendAvailable.length > 0;
    const baseHours = hasBackendData ? backendAvailable : this.bookingDataService.getWorkingHours();
    console.log('5. Horas base a usar:', baseHours);

    const hours: string[] = [];

    // Obtener la fecha seleccionada
    const selectedDateStr = this.selectedDate();
    if (!selectedDateStr) {
      console.warn('generateAvailableHours llamado sin fecha seleccionada');
      return;
    }

    // Crear un objeto de fecha para hoy en la zona horaria local para comparar
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const todayStr = `${year}-${month}-${day}`;

    const isToday = selectedDateStr === todayStr;
    console.log('6. ¿Es hoy?:', isToday, 'Fecha seleccionada:', selectedDateStr, 'Fecha actual:', todayStr);

    for (const hourStr of baseHours) {
      if (isToday) {
        const [hour, minutes] = hourStr.split(':').map(Number);
        const currentHour = now.getHours();
        const currentMinutes = now.getMinutes();

        // Si la hora ya pasó (más de 30 mins de cortesía), saltar
        // Ejemplo: son las 10:45. La cita de las 10:00 ya no sale.
        // La cita de las 11:00 sí sale.
        const slotTime = hour * 60 + minutes;
        const currentTime = currentHour * 60 + currentMinutes;

        if (slotTime < (currentTime - 30)) {
          console.log(`Filtro hoy: ${hourStr} descartada (${slotTime} < ${currentTime - 30})`);
          continue;
        }
      }

      hours.push(hourStr);
    }

    console.log('7. Horarios finales a mostrar:', hours);
    this.availableHours.set(hours);
  }

  selectHour(hour: string) {
    this.selectedHour.set(hour);
    this.errorMessage = ''; // Limpiar errores al seleccionar una hora
    this.scrollToData();
  }

  private scrollToHours() {
    setTimeout(() => {
      this.hoursSection?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
  }

  private scrollToData() {
    setTimeout(() => {
      this.dataSection?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
  }

  private scrollToConfirmation() {
    setTimeout(() => {
      this.confirmationSection?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
  }

  onSubmit() {
    if (this.bookingForm.invalid || !this.selectedDate() || !this.selectedHour()) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.submitted.set(false);
    this.errorMessage = '';

    const phone = this.formatPhone(this.bookingForm.value.phone);

    const formData = {
      token: 'AGENDA2025',
      nombre: this.bookingForm.value.name.trim(),
      telefono: phone,
      correo: this.bookingForm.value.email.trim(),
      servicio: this.bookingForm.value.service,
      fecha: this.selectedDate(),
      hora: this.selectedHour()
    };

    this.appointmentService.bookAppointment(formData)
      .pipe(
        finalize(() => this.isSubmitting.set(false))
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

          if (resData?.success) {
            this.handleSuccess();
          } else {
            this.errorMessage = resData?.message || 'Hubo un problema al procesar la cita.';
          }
        },
        error: (err) => {
          console.error('Error al agendar cita tras reintentos:', err);
          this.handleSuccess(true);
        }
      });
  }

  private formatPhone(phone: string): string {
    let cleaned = phone.trim();
    if (!cleaned.startsWith('+')) {
      if (cleaned.startsWith('521')) {
        cleaned = '+' + cleaned;
      } else {
        cleaned = '+521' + cleaned;
      }
    }
    return cleaned;
  }

  private handleSuccess(isMaybe = false) {
    this.submitted.set(true);
    this.isSubmitting.set(false);
    this.bookingForm.reset();
    this.selectedDate.set(null);
    this.selectedHour.set(null);

    this.errorMessage = isMaybe
      ? 'Nota: La respuesta del servidor fue inusual, pero tu cita probablemente fue enviada. Revisa tu correo y confirma por WhatsApp al +1 267 313 6057.'
      : '¡Cita enviada! Revisa tu correo y recuerda confirmar por WhatsApp escribiendo "Hola" al +1 267 313 6057.';

    this.scrollToConfirmation();
    setTimeout(() => {
      this.submitted.set(false);
      this.errorMessage = '';
    }, 15000);
  }
}
