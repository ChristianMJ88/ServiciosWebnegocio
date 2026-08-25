import { Component, signal, ElementRef, ViewChild, inject, OnInit, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  AppointmentService,
  ConsultaFranjasRequest,
  CrearCitaBackendRequest,
  CrearCitasMultiplesBackendRequest,
  FranjaDisponible,
  PrestadorPublico
} from '../../services/appointment.service';
import {
  BookingDataService,
  ServicioCatalogo,
  SucursalCatalogo
} from '../../services/booking-data.service';
import type { CalendarOptions, DateSelectArg } from '@fullcalendar/core';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { BookingCalendarComponent } from './booking-calendar.component';

interface ItinerarioReservaItem {
  servicio: ServicioCatalogo;
  slot: FranjaDisponible;
  staff: PrestadorPublico | null;
}

interface ItinerarioReserva {
  id: string;
  items: ItinerarioReservaItem[];
  inicio: string;
  fin: string;
  totalPrecio: number;
  totalMinutos: number;
  usaMultiplesStaff: boolean;
}

interface OpcionInicioReserva {
  inicio: string;
  hora: string;
  finHora: string;
  itinerary: ItinerarioReserva;
}

interface OpcionHorarioServicio {
  slot: FranjaDisponible;
  staff: PrestadorPublico | null;
}

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BookingCalendarComponent],
  templateUrl: './booking.component.html',
  styleUrls: ['./booking.component.css']
})
export class BookingComponent implements OnInit {
  private readonly mobileBreakpoint = '(max-width: 767.98px)';
  private fb = inject(FormBuilder);
  private appointmentService = inject(AppointmentService);
  private bookingDataService = inject(BookingDataService);
  private breakpointObserver = inject(BreakpointObserver);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  readonly tenantContext = inject(TenantContextService);
  private readonly hoy = this.normalizarFecha(new Date());
  private readonly fechaMaximaReserva = this.sumarDias(this.hoy, 30);

  @ViewChild('hoursSection') hoursSection!: ElementRef;
  @ViewChild('dataSection') dataSection!: ElementRef;
  @ViewChild('confirmationSection') confirmationSection!: ElementRef;

  bookingForm: FormGroup = this.fb.group({
    branchId: [null, Validators.required],
    groupId: [null],
    subgroupId: [null],
    serviceId: [null, Validators.required],
    name: ['', [Validators.required, Validators.minLength(3)]],
    phone: ['', [Validators.required, Validators.pattern('^[0-9+ ]{10,15}$')]],
    email: ['', [Validators.required, Validators.email]]
  });

  submitted = signal(false);
  isSubmitting = signal(false);
  loadingSlots = signal(false);
  loadingCatalog = signal(false);
  isMobile = signal(this.breakpointObserver.isMatched(this.mobileBreakpoint));
  schedulingMode = signal<'consecutive' | 'separated'>('consecutive');
  selectedDate = signal<string | null>(null);
  selectedHour = signal<string | null>(null);
  selectedGroupId = signal<number | null>(null);
  selectedSubgroupId = signal<number | null>(null);
  availableSlots = signal<FranjaDisponible[]>([]);
  selectedServices = signal<ServicioCatalogo[]>([]);
  staffByService = signal<Record<number, PrestadorPublico[]>>({});
  slotsByService = signal<Record<number, FranjaDisponible[]>>({});
  selectedSeparatedSlots = signal<Record<number, FranjaDisponible>>({});
  availableItineraries = signal<ItinerarioReserva[]>([]);
  selectedItineraryId = signal<string | null>(null);
  branches = signal<SucursalCatalogo[]>([]);
  services = signal<ServicioCatalogo[]>([]);
  errorMessage = '';
  readonly availableGroups = computed(() => {
    const groups = new Map<number, { id: number; nombre: string }>();
    for (const service of this.services()) {
      if (service.grupoId && service.grupoNombre) {
        groups.set(service.grupoId, { id: service.grupoId, nombre: service.grupoNombre });
      }
    }
    return Array.from(groups.values()).sort((a, b) => a.nombre.localeCompare(b.nombre));
  });
  readonly availableSubgroups = computed(() => {
    const selectedGroupId = this.selectedGroupId();
    const subgroups = new Map<number, { id: number; nombre: string }>();
    for (const service of this.services()) {
      if (service.subgrupoId && service.subgrupoNombre && (!selectedGroupId || service.grupoId === selectedGroupId)) {
        subgroups.set(service.subgrupoId, { id: service.subgrupoId, nombre: service.subgrupoNombre });
      }
    }
    return Array.from(subgroups.values()).sort((a, b) => a.nombre.localeCompare(b.nombre));
  });
  readonly filteredServices = computed(() => {
    return this.services();
  });
  readonly selectedItinerary = computed(() => {
    if (this.schedulingMode() === 'separated') {
      return this.separatedItinerary();
    }

    const itineraryId = this.selectedItineraryId();
    if (!itineraryId) {
      return null;
    }
    return this.availableItineraries().find(itinerary => itinerary.id === itineraryId) ?? null;
  });
  readonly availableStartOptions = computed<OpcionInicioReserva[]>(() => {
    const grouped = new Map<string, ItinerarioReserva[]>();

    for (const itinerary of this.availableItineraries()) {
      const start = itinerary.items[0]?.slot.inicio;
      if (!start) {
        continue;
      }
      grouped.set(start, [...(grouped.get(start) ?? []), itinerary]);
    }

    return Array.from(grouped.entries())
      .map(([inicio, itineraries]) => {
        const itinerary = [...itineraries].sort((a, b) => this.compareItineraries(a, b))[0];
        return {
          inicio,
          hora: itinerary.items[0]?.slot.hora ?? '',
          finHora: this.formatearHora(itinerary.fin),
          itinerary
        };
      })
      .sort((a, b) => new Date(a.inicio).getTime() - new Date(b.inicio).getTime());
  });
  readonly separatedCurrentIndex = computed(() => {
    const services = this.selectedServices();
    const selected = this.selectedSeparatedSlots();
    return services.findIndex(service => !selected[service.id]);
  });
  readonly separatedCurrentService = computed(() => {
    const index = this.separatedCurrentIndex();
    return index === -1 ? null : this.selectedServices()[index] ?? null;
  });
  readonly separatedItinerary = computed<ItinerarioReserva | null>(() => {
    const services = this.selectedServices();
    const selected = this.selectedSeparatedSlots();

    if (!services.length || services.some(service => !selected[service.id])) {
      return null;
    }

    const items = services
      .map(service => {
        const slot = selected[service.id];
        const staff = (this.staffByService()[service.id] ?? []).find(item => item.usuarioId === slot.prestadorId) ?? null;
        return { servicio: service, slot, staff };
      });

    const inicio = items[0]?.slot.inicio ?? '';
    const fin = items[items.length - 1]?.slot.fin ?? '';
    const staffIds = new Set(items.map(item => item.slot.prestadorId).filter(Boolean));

    return {
      id: items.map(item => `${item.servicio.id}-${item.slot.inicio}`).join('|'),
      items,
      inicio,
      fin,
      totalPrecio: items.reduce((total, item) => total + item.servicio.precio, 0),
      totalMinutos: items.reduce((total, item) => {
        return total + item.servicio.duracionMinutos + item.servicio.bufferAntesMinutos + item.servicio.bufferDespuesMinutos;
      }, 0),
      usaMultiplesStaff: staffIds.size > 1
    };
  });
  readonly separatedCurrentOptions = computed<OpcionHorarioServicio[]>(() => {
    const service = this.separatedCurrentService();
    if (!service) {
      return [];
    }

    const selected = this.selectedSeparatedSlots();
    const currentIndex = this.selectedServices().findIndex(item => item.id === service.id);
    const previousService = currentIndex > 0 ? this.selectedServices()[currentIndex - 1] : null;
    const previousSlot = previousService ? selected[previousService.id] : null;
    const minimumStart = previousSlot ? new Date(previousSlot.fin).getTime() : null;

    return (this.slotsByService()[service.id] ?? [])
      .filter(slot => (minimumStart === null ? true : new Date(slot.inicio).getTime() >= minimumStart))
      .map(slot => ({
        slot,
        staff: (this.staffByService()[service.id] ?? []).find(item => item.usuarioId === slot.prestadorId) ?? null
      }));
  });
  readonly mobileWeekStart = signal(this.inicioSemana(this.hoy));
  readonly mobileMonthLabel = computed(() => {
    const inicio = this.mobileWeekStart();
    const fin = this.sumarDias(inicio, 6);
    const formatter = new Intl.DateTimeFormat('es-MX', { month: 'long', year: 'numeric' });
    const inicioTexto = formatter.format(inicio);
    const finTexto = formatter.format(fin);
    return inicioTexto === finTexto ? inicioTexto : `${inicioTexto} - ${finTexto}`;
  });
  readonly canGoPrevWeek = computed(() => this.mobileWeekStart().getTime() > this.inicioSemana(this.hoy).getTime());
  readonly canGoNextWeek = computed(() => this.mobileWeekStart().getTime() < this.inicioSemana(this.fechaMaximaReserva).getTime());
  readonly mobileWeekDays = computed(() =>
    Array.from({ length: 7 }, (_, index) => {
      const fecha = this.sumarDias(this.mobileWeekStart(), index);
      const fechaIso = this.formatearFechaLocal(fecha);
      const disponible = this.esFechaReservable(fecha);
      return {
        fecha,
        fechaIso,
        diaCorto: new Intl.DateTimeFormat('es-MX', { weekday: 'short' }).format(fecha).replace('.', ''),
        diaNumero: fecha.getDate(),
        disponible,
        seleccionada: this.selectedDate() === fechaIso,
        esHoy: fechaIso === this.formatearFechaLocal(this.hoy)
      };
    })
  );

  calendarOptions: CalendarOptions = {
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
    this.breakpointObserver
      .observe(this.mobileBreakpoint)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ matches }) => {
        this.isMobile.set(matches);
      });
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

  get selectedSlot(): FranjaDisponible | undefined {
    return this.selectedItinerary()?.items[0]?.slot;
  }

  onBranchChange(rawBranchId: string) {
    const branchId = rawBranchId ? Number(rawBranchId) : null;
    this.bookingForm.patchValue({ branchId, groupId: null, subgroupId: null, serviceId: null });
    this.selectedGroupId.set(null);
    this.selectedSubgroupId.set(null);
    this.services.set([]);
    this.selectedServices.set([]);
    this.staffByService.set({});
    this.resetAvailabilityFlow();

    if (branchId) {
      this.loadServices(branchId);
    }
  }

  onGroupChange(rawGroupId: string) {
    const groupId = rawGroupId ? Number(rawGroupId) : null;
    this.bookingForm.patchValue({ groupId, subgroupId: null, serviceId: null });
    this.selectedGroupId.set(groupId);
    this.selectedSubgroupId.set(null);
    this.resetAvailabilityFlow();
  }

  onSubgroupChange(rawSubgroupId: string) {
    const subgroupId = rawSubgroupId ? Number(rawSubgroupId) : null;
    this.bookingForm.patchValue({ subgroupId, serviceId: null });
    this.selectedSubgroupId.set(subgroupId);
    this.resetAvailabilityFlow();
  }

  onServiceChange(rawServiceId: string) {
    const serviceId = rawServiceId ? Number(rawServiceId) : null;
    this.bookingForm.patchValue({ serviceId });
    this.resetAvailabilityFlow();
  }

  addSelectedService() {
    const service = this.selectedService;
    if (!service) {
      this.errorMessage = 'Selecciona un servicio antes de agregarlo.';
      return;
    }

    if (this.selectedServices().some(item => item.id === service.id)) {
      this.errorMessage = 'Ese servicio ya está en la selección.';
      return;
    }

    this.selectedServices.update(actuales => [...actuales, service]);
    this.cargarStaffServicio(service);
    this.resetAvailabilityFlow();
    this.errorMessage = '';
  }

  removeSelectedService(serviceId: number) {
    this.selectedServices.update(actuales => actuales.filter(service => service.id !== serviceId));
    this.staffByService.update(actual => {
      const copia = { ...actual };
      delete copia[serviceId];
      return copia;
    });
    this.resetAvailabilityFlow();
  }

  onDateSelect(date: string) {
    if (!this.hasCatalogSelection()) {
      this.errorMessage = 'Selecciona primero una sucursal y un servicio.';
      return;
    }

    this.selectedDate.set(date);
    this.sincronizarSemanaMovil(date);
    this.selectedHour.set(null);
    this.selectedItineraryId.set(null);
    this.loadingSlots.set(true);
    this.errorMessage = '';
    this.availableSlots.set([]);

    const requests = this.selectedServices().map(service => {
      const request: ConsultaFranjasRequest = {
        empresaId: this.tenantContext.empresaId() ?? 1,
        sucursalId: Number(this.bookingForm.value.branchId),
        servicioId: service.id,
        fecha: date
      };
      return this.appointmentService.getAvailableSlots(request);
    });

    forkJoin(requests)
      .pipe(
        finalize(() => {
          this.loadingSlots.set(false);
          setTimeout(() => this.scrollToHours(), 100);
        })
      )
      .subscribe({
        next: (slotsByService) => {
          const itineraries = this.buildItineraries(this.selectedServices(), slotsByService, 15);
          const slotMap = this.selectedServices().reduce<Record<number, FranjaDisponible[]>>((acc, service, index) => {
            acc[service.id] = slotsByService[index] ?? [];
            return acc;
          }, {});

          this.slotsByService.set(slotMap);
          this.availableSlots.set(slotsByService[0] ?? []);
          this.availableItineraries.set(itineraries);
          this.selectedItineraryId.set(null);
          this.selectedHour.set(null);
          this.selectedSeparatedSlots.set({});
          if (this.schedulingMode() === 'consecutive' && !itineraries.length) {
            this.errorMessage = this.hasMultipleServices()
              ? 'No encontramos un itinerario continuo para ese día. Puedes probar con horarios separados.'
              : 'No encontramos horarios disponibles para ese día.';
          }
        },
        error: (err) => {
          console.error('ERROR en la petición:', err);
          this.errorMessage = err?.message || 'No se pudieron cargar los horarios disponibles.';
          this.availableSlots.set([]);
          this.slotsByService.set({});
          this.selectedSeparatedSlots.set({});
          this.availableItineraries.set([]);
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
    const itinerary = this.availableItineraries().find(option => option.items[0]?.slot.inicio === slot.inicio);
    if (itinerary) {
      this.selectItinerary(itinerary);
      return;
    }

    this.selectedHour.set(slot.inicio);
    this.errorMessage = '';
    this.scrollToData();
  }

  selectItinerary(itinerary: ItinerarioReserva) {
    this.selectedItineraryId.set(itinerary.id);
    this.selectedHour.set(itinerary.items[0]?.slot.inicio ?? null);
    this.errorMessage = '';
    this.scrollToData();
  }

  selectStartOption(option: OpcionInicioReserva) {
    this.selectItinerary(option.itinerary);
  }

  setSchedulingMode(mode: 'consecutive' | 'separated') {
    this.schedulingMode.set(mode);
    this.selectedItineraryId.set(null);
    this.selectedHour.set(null);
    this.selectedSeparatedSlots.set({});
    this.errorMessage = mode === 'consecutive' && !this.availableItineraries().length && this.selectedDate()
      ? 'No encontramos un itinerario continuo para ese día. Puedes probar con horarios separados.'
      : '';
  }

  selectSeparatedSlot(serviceId: number, slot: FranjaDisponible) {
    this.selectedSeparatedSlots.update(actual => {
      const next = { ...actual, [serviceId]: slot };
      const firstService = this.selectedServices()[0];
      this.selectedHour.set(firstService ? (next[firstService.id]?.inicio ?? slot.inicio) : slot.inicio);
      return next;
    });
    this.errorMessage = '';

    if (this.separatedCurrentIndex() === -1) {
      this.scrollToData();
    }
  }

  resetSeparatedFrom(index: number) {
    const services = this.selectedServices().slice(index);
    this.selectedSeparatedSlots.update(actual => {
      const next = { ...actual };
      for (const service of services) {
        delete next[service.id];
      }
      return next;
    });
    this.selectedHour.set(null);
    this.errorMessage = '';
  }

  selectMobileDay(date: string) {
    if (!this.hasCatalogSelection()) {
      this.errorMessage = 'Selecciona primero una sucursal y un servicio.';
      return;
    }

    const fecha = this.parsearFechaLocal(date);
    if (!this.esFechaReservable(fecha)) {
      return;
    }

    this.onDateSelect(date);
  }

  moveMobileWeek(offset: number) {
    const base = this.mobileWeekStart();
    const propuesta = this.inicioSemana(this.sumarDias(base, offset * 7));
    const inicioMinimo = this.inicioSemana(this.hoy);
    const inicioMaximo = this.inicioSemana(this.fechaMaximaReserva);

    if (propuesta < inicioMinimo) {
      this.mobileWeekStart.set(inicioMinimo);
      return;
    }

    if (propuesta > inicioMaximo) {
      this.mobileWeekStart.set(inicioMaximo);
      return;
    }

    this.mobileWeekStart.set(propuesta);
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
    const itinerary = this.selectedItinerary();
    if (this.bookingForm.invalid || !this.selectedDate() || !itinerary || !this.selectedSlot) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.submitted.set(false);
    this.errorMessage = '';

    const phone = this.formatPhone(this.bookingForm.value.phone);

    const singleItem = itinerary.items.length === 1 ? itinerary.items[0] : null;
    const request$ = singleItem
      ? this.appointmentService.bookAppointment({
          empresaId: this.tenantContext.empresaId() ?? 1,
          sucursalId: Number(this.bookingForm.value.branchId),
          servicioId: singleItem.servicio.id,
          prestadorId: singleItem.slot.prestadorId,
          nombreCliente: this.bookingForm.value.name.trim(),
          telefonoCliente: phone,
          correoCliente: this.bookingForm.value.email.trim(),
          inicio: singleItem.slot.inicio,
          notas: this.selectedServices().map(service => service.nombre).join(', ') || singleItem.servicio.nombre
        } as CrearCitaBackendRequest)
      : this.appointmentService.bookMultipleAppointments({
          empresaId: this.tenantContext.empresaId() ?? 1,
          sucursalId: Number(this.bookingForm.value.branchId),
          nombreCliente: this.bookingForm.value.name.trim(),
          telefonoCliente: phone,
          correoCliente: this.bookingForm.value.email.trim(),
          notas: this.selectedServices().map(service => service.nombre).join(', '),
          items: itinerary.items.map(item => ({
            servicioId: item.servicio.id,
            prestadorId: item.slot.prestadorId,
            inicio: item.slot.inicio
          }))
        } as CrearCitasMultiplesBackendRequest);

    request$
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

          if (response?.id || response?.total) {
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
    this.selectedGroupId.set(null);
    this.selectedSubgroupId.set(null);
    this.selectedDate.set(null);
    this.selectedHour.set(null);
    this.selectedServices.set([]);
    this.staffByService.set({});
    this.slotsByService.set({});
    this.selectedSeparatedSlots.set({});
    this.availableSlots.set([]);
    this.availableItineraries.set([]);
    this.selectedItineraryId.set(null);
    this.schedulingMode.set('consecutive');
    this.mobileWeekStart.set(this.inicioSemana(this.hoy));
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
            this.selectedGroupId.set(null);
            this.selectedSubgroupId.set(null);
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
          this.selectedGroupId.set(null);
          this.selectedSubgroupId.set(null);
          if (services.length === 1) {
            this.bookingForm.patchValue({ serviceId: services[0].id });
          } else {
            this.bookingForm.patchValue({ groupId: null, subgroupId: null, serviceId: null });
          }
          this.hidratarServiciosDesdeQuery();
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
    this.slotsByService.set({});
    this.selectedSeparatedSlots.set({});
    this.availableSlots.set([]);
    this.availableItineraries.set([]);
    this.selectedItineraryId.set(null);
    this.schedulingMode.set('consecutive');
    this.submitted.set(false);
    this.errorMessage = '';
    this.mobileWeekStart.set(this.inicioSemana(this.hoy));
  }

  hasCatalogSelection(): boolean {
    return Boolean(this.bookingForm.value.branchId && this.selectedServices().length);
  }

  hasMultipleServices(): boolean {
    return this.selectedServices().length > 1;
  }

  selectedSeparatedSlot(serviceId: number): FranjaDisponible | null {
    return this.selectedSeparatedSlots()[serviceId] ?? null;
  }

  currentStepLabel(): string {
    if (!this.hasCatalogSelection()) {
      return 'Paso 1 de 4';
    }

    if (!this.selectedDate()) {
      return 'Paso 2 de 4';
    }

    if (!this.selectedItinerary()) {
      return 'Paso 3 de 4';
    }

    return 'Paso 4 de 4';
  }

  itineraryEndHour(itinerary: ItinerarioReserva | null | undefined): string {
    if (!itinerary?.fin) {
      return '';
    }

    return this.formatearHora(itinerary.fin);
  }

  itineraryServiceNames(itinerary: ItinerarioReserva | null | undefined): string[] {
    return itinerary?.items.map(item => item.servicio.nombre) ?? [];
  }

  itineraryStaffSummary(itinerary: ItinerarioReserva | null | undefined): string {
    if (!itinerary) {
      return '';
    }

    const nombres = Array.from(
      new Set(
        itinerary.items
          .map(item => item.staff?.nombreMostrar)
          .filter((nombre): nombre is string => !!nombre)
      )
    );

    if (nombres.length <= 1) {
      return nombres[0] || 'Staff disponible';
    }

    return `${nombres.length} especialistas disponibles`;
  }

  selectedServicesPriceTotal(): number {
    return this.selectedServices().reduce((total, service) => total + service.precio, 0);
  }

  private sincronizarSemanaMovil(date: string) {
    this.mobileWeekStart.set(this.inicioSemana(this.parsearFechaLocal(date)));
  }

  private cargarStaffServicio(service: ServicioCatalogo) {
    this.appointmentService.getPublicStaff(
      this.tenantContext.empresaId() ?? 1,
      Number(this.bookingForm.value.branchId),
      service.id
    ).subscribe({
      next: staff => {
        this.staffByService.update(actual => ({ ...actual, [service.id]: staff }));
      },
      error: () => undefined
    });
  }

  private buildItineraries(
    selectedServices: ServicioCatalogo[],
    slotsByService: FranjaDisponible[][],
    maxGapMinutes: number | null
  ): ItinerarioReserva[] {
    const itineraries: ItinerarioReserva[] = [];
    const staffMap = this.staffByService();

    const walk = (index: number, previousEnd: string | null, items: ItinerarioReservaItem[]) => {
      if (itineraries.length >= 18) {
        return;
      }

      if (index >= selectedServices.length) {
        const inicio = items[0]?.slot.inicio ?? '';
        const fin = items[items.length - 1]?.slot.fin ?? '';
        const staffIds = new Set(items.map(item => item.slot.prestadorId).filter(Boolean));
        itineraries.push({
          id: items.map(item => `${item.servicio.id}-${item.slot.inicio}`).join('|'),
          items: [...items],
          inicio,
          fin,
          totalPrecio: items.reduce((total, item) => total + item.servicio.precio, 0),
          totalMinutos: items.reduce((total, item) => {
            return total + item.servicio.duracionMinutos + item.servicio.bufferAntesMinutos + item.servicio.bufferDespuesMinutos;
          }, 0),
          usaMultiplesStaff: staffIds.size > 1
        });
        return;
      }

      const service = selectedServices[index];
      for (const slot of slotsByService[index] ?? []) {
        if (previousEnd && new Date(slot.inicio).getTime() < new Date(previousEnd).getTime()) {
          continue;
        }

        if (previousEnd && maxGapMinutes !== null) {
          const gap = new Date(slot.inicio).getTime() - new Date(previousEnd).getTime();
          if (gap > maxGapMinutes * 60_000) {
            continue;
          }
        }

        const staff = (staffMap[service.id] ?? []).find(item => item.usuarioId === slot.prestadorId) ?? null;
        items.push({ servicio: service, slot, staff });
        walk(index + 1, slot.fin, items);
        items.pop();
      }
    };

    walk(0, null, []);
    return itineraries;
  }

  private compareItineraries(a: ItinerarioReserva, b: ItinerarioReserva): number {
    if (a.usaMultiplesStaff !== b.usaMultiplesStaff) {
      return a.usaMultiplesStaff ? 1 : -1;
    }

    if (a.totalMinutos !== b.totalMinutos) {
      return a.totalMinutos - b.totalMinutos;
    }

    return new Date(a.fin).getTime() - new Date(b.fin).getTime();
  }

  private hidratarServiciosDesdeQuery() {
    const serviceParams = this.route.snapshot.queryParamMap.get('servicios') ?? this.route.snapshot.queryParamMap.get('servicio');
    if (!serviceParams) {
      return;
    }

    const tokens = serviceParams.split(',').map(token => token.trim().toLowerCase()).filter(Boolean);
    if (!tokens.length) {
      return;
    }

    const encontrados = this.services().filter(service => {
      return tokens.includes(String(service.id).toLowerCase()) || (service.slug ? tokens.includes(service.slug.toLowerCase()) : false);
    });

    if (!encontrados.length) {
      return;
    }

    this.selectedServices.set(encontrados);
    this.bookingForm.patchValue({ serviceId: encontrados[0].id });
    encontrados.forEach(service => this.cargarStaffServicio(service));
  }

  private esFechaReservable(fecha: Date): boolean {
    const normalizada = this.normalizarFecha(fecha);
    const dia = normalizada.getDay();
    return normalizada >= this.hoy && normalizada <= this.fechaMaximaReserva && dia !== 0;
  }

  private inicioSemana(fecha: Date): Date {
    const normalizada = this.normalizarFecha(fecha);
    const inicio = new Date(normalizada);
    inicio.setDate(normalizada.getDate() - normalizada.getDay());
    return this.normalizarFecha(inicio);
  }

  private sumarDias(fecha: Date, dias: number): Date {
    const resultado = new Date(fecha);
    resultado.setDate(resultado.getDate() + dias);
    return this.normalizarFecha(resultado);
  }

  private normalizarFecha(fecha: Date): Date {
    const normalizada = new Date(fecha);
    normalizada.setHours(0, 0, 0, 0);
    return normalizada;
  }

  private formatearFechaLocal(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = String(fecha.getMonth() + 1).padStart(2, '0');
    const day = String(fecha.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private parsearFechaLocal(fecha: string): Date {
    const [year, month, day] = fecha.split('-').map(Number);
    return this.normalizarFecha(new Date(year, (month || 1) - 1, day || 1));
  }

  private formatearHora(fechaIso: string): string {
    const fecha = new Date(fechaIso);
    return new Intl.DateTimeFormat('es-MX', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }).format(fecha);
  }
}
