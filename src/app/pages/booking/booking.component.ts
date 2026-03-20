import { Component, signal, ElementRef, ViewChild, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  AppointmentService,
  ConsultaFranjasRequest,
  CrearCitaBackendRequest,
  FranjaDisponible
} from '../../services/appointment.service';
import {
  BookingDataService,
  ServicioCatalogo,
  SucursalCatalogo
} from '../../services/booking-data.service';
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
export class BookingComponent implements OnInit {
  private fb = inject(FormBuilder);
  private appointmentService = inject(AppointmentService);
  private bookingDataService = inject(BookingDataService);

  @ViewChild('hoursSection') hoursSection!: ElementRef;
  @ViewChild('dataSection') dataSection!: ElementRef;
  @ViewChild('confirmationSection') confirmationSection!: ElementRef;

  bookingForm: FormGroup = this.fb.group({
    branchId: [null, Validators.required],
    serviceId: [null, Validators.required],
    name: ['', [Validators.required, Validators.minLength(3)]],
    phone: ['', [Validators.required, Validators.pattern('^[0-9+ ]{10,15}$')]],
    email: ['', [Validators.required, Validators.email]]
  });

  submitted = signal(false);
  isSubmitting = signal(false);
  loadingSlots = signal(false);
  loadingCatalog = signal(false);
  selectedDate = signal<string | null>(null);
  selectedHour = signal<string | null>(null);
  availableSlots = signal<FranjaDisponible[]>([]);
  branches = signal<SucursalCatalogo[]>([]);
  services = signal<ServicioCatalogo[]>([]);
  errorMessage = '';

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
      if (!this.hasCatalogSelection()) {
        return false;
      }
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

  ngOnInit(): void {
    this.loadBranches();
  }

  get f() { return this.bookingForm.controls; }

  get selectedBranch(): SucursalCatalogo | undefined {
    const branchId = Number(this.bookingForm.value.branchId);
    return this.branches().find(branch => branch.id === branchId);
  }

  get selectedService(): ServicioCatalogo | undefined {
    const serviceId = Number(this.bookingForm.value.serviceId);
    return this.services().find(service => service.id === serviceId);
  }

  onBranchChange(rawBranchId: string) {
    const branchId = rawBranchId ? Number(rawBranchId) : null;
    this.bookingForm.patchValue({ branchId, serviceId: null });
    this.services.set([]);
    this.resetAvailabilityFlow();

    if (branchId) {
      this.loadServices(branchId);
    }
  }

  onServiceChange(rawServiceId: string) {
    const serviceId = rawServiceId ? Number(rawServiceId) : null;
    this.bookingForm.patchValue({ serviceId });
    this.resetAvailabilityFlow();
  }

  onDateSelect(date: string) {
    if (!this.hasCatalogSelection()) {
      this.errorMessage = 'Selecciona primero una sucursal y un servicio.';
      return;
    }

    this.selectedDate.set(date);
    this.selectedHour.set(null);
    this.loadingSlots.set(true);
    this.errorMessage = '';
    this.availableSlots.set([]);

    const request: ConsultaFranjasRequest = {
      empresaId: 1,
      sucursalId: Number(this.bookingForm.value.branchId),
      servicioId: Number(this.bookingForm.value.serviceId),
      fecha: date
    };

    this.appointmentService.getAvailableSlots(request)
      .pipe(
        finalize(() => {
          this.loadingSlots.set(false);
          setTimeout(() => this.scrollToHours(), 100);
        })
      )
      .subscribe({
        next: (slots) => {
          this.generateAvailableSlots(slots);
        },
        error: (err) => {
          console.error('ERROR en la petición:', err);
          this.errorMessage = err?.message || 'No se pudieron cargar los horarios disponibles.';
          this.availableSlots.set([]);
        }
      });
  }

  private generateAvailableSlots(backendAvailable: FranjaDisponible[]) {
    const slots: FranjaDisponible[] = [];
    const selectedDateStr = this.selectedDate();
    if (!selectedDateStr) {
      return;
    }

    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const todayStr = `${year}-${month}-${day}`;

    const isToday = selectedDateStr === todayStr;

    for (const slot of backendAvailable) {
      if (isToday) {
        const slotDate = new Date(slot.inicio);
        const slotTime = slotDate.getHours() * 60 + slotDate.getMinutes();
        const currentTime = now.getHours() * 60 + now.getMinutes();
        if (slotTime < (currentTime - 30)) {
          continue;
        }
      }

      slots.push(slot);
    }

    this.availableSlots.set(slots);
  }

  selectHour(slot: FranjaDisponible) {
    this.selectedHour.set(slot.inicio);
    this.errorMessage = '';
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
    if (this.bookingForm.invalid || !this.selectedDate() || !this.selectedHour() || !this.selectedService) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.submitted.set(false);
    this.errorMessage = '';

    const phone = this.formatPhone(this.bookingForm.value.phone);

    const formData: CrearCitaBackendRequest = {
      empresaId: 1,
      sucursalId: Number(this.bookingForm.value.branchId),
      servicioId: Number(this.bookingForm.value.serviceId),
      nombreCliente: this.bookingForm.value.name.trim(),
      telefonoCliente: phone,
      correoCliente: this.bookingForm.value.email.trim(),
      inicio: this.selectedHour()!,
      notas: this.selectedService?.nombre ?? null
    };

    this.appointmentService.bookAppointment(formData)
      .pipe(
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: (response: any) => {
          if (!response) {
            this.handleSuccess();
            return;
          }

          if (typeof response === 'string') {
            try {
              const legacyResponse = JSON.parse(response);
              if (legacyResponse?.success) {
                this.handleSuccess(legacyResponse?.message || 'Tu cita fue registrada correctamente.');
              } else {
                this.errorMessage = legacyResponse?.message || 'Hubo un problema al procesar la cita.';
              }
            } catch {
              this.handleSuccess('Tu cita fue registrada correctamente.');
            }
            return;
          }

          if (response?.id) {
            this.handleSuccess(response?.mensaje || 'Tu cita fue registrada correctamente.');
          } else {
            this.errorMessage = response?.mensaje || 'Hubo un problema al procesar la cita.';
          }
        },
        error: (err) => {
          console.error('Error al agendar cita:', err);
          this.errorMessage = err?.message || 'No se pudo completar la reserva.';
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

  private handleSuccess(message = 'Tu cita fue registrada correctamente.') {
    this.submitted.set(true);
    this.isSubmitting.set(false);
    this.bookingForm.reset();
    this.selectedDate.set(null);
    this.selectedHour.set(null);
    this.availableSlots.set([]);
    this.errorMessage = message;

    if (this.branches().length === 1) {
      const branchId = this.branches()[0].id;
      this.bookingForm.patchValue({ branchId });
      this.loadServices(branchId);
    }

    this.scrollToConfirmation();
    setTimeout(() => {
      this.submitted.set(false);
      this.errorMessage = '';
    }, 15000);
  }

  private loadBranches() {
    this.loadingCatalog.set(true);
    this.bookingDataService.getBranches()
      .pipe(finalize(() => this.loadingCatalog.set(false)))
      .subscribe({
        next: (branches) => {
          this.branches.set(branches);
          if (branches.length === 1) {
            this.bookingForm.patchValue({ branchId: branches[0].id });
            this.loadServices(branches[0].id);
          }
        },
        error: (err) => {
          console.error('Error cargando sucursales:', err);
          this.errorMessage = 'No se pudo cargar el catálogo de sucursales.';
        }
      });
  }

  private loadServices(branchId: number) {
    this.loadingCatalog.set(true);
    this.bookingDataService.getServices(branchId)
      .pipe(finalize(() => this.loadingCatalog.set(false)))
      .subscribe({
        next: (services) => {
          this.services.set(services);
          if (services.length === 1) {
            this.bookingForm.patchValue({ serviceId: services[0].id });
          }
        },
        error: (err) => {
          console.error('Error cargando servicios:', err);
          this.errorMessage = 'No se pudo cargar el catálogo de servicios.';
        }
      });
  }

  private resetAvailabilityFlow() {
    this.selectedDate.set(null);
    this.selectedHour.set(null);
    this.availableSlots.set([]);
    this.submitted.set(false);
    this.errorMessage = '';
  }

  hasCatalogSelection(): boolean {
    return Boolean(this.bookingForm.value.branchId && this.bookingForm.value.serviceId);
  }
}
