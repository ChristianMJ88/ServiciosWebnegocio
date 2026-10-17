import { ProductCartService } from '../../services/product-cart.service';
import { Component, signal, ElementRef, ViewChild, inject, OnInit, computed, DestroyRef, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  AppointmentService,
  CitaCreadaBackendResponse,
  CitasMultiplesCreadasResponse,
  CotizacionCitas,
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
import { Observable, forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { ReservationCartService } from '../../services/reservation-cart.service';
import { ReservationCartComponent } from '../../shared/components/reservation-cart/reservation-cart.component';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
import { BookingCalendarComponent } from './booking-calendar.component';
import { AppointmentCheckoutDraftService } from '../../services/appointment-checkout-draft.service';

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
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BookingCalendarComponent, ReservationCartComponent, MoneyDisplayPipe],
  templateUrl: './booking.component.html',
  styleUrls: ['./booking.component.css', './booking-experience.css']
})
export class BookingComponent implements OnInit {
  private readonly mobileBreakpoint = '(max-width: 767.98px)';
  private fb = inject(FormBuilder);
  private appointmentService = inject(AppointmentService);
  private bookingDataService = inject(BookingDataService);
  private breakpointObserver = inject(BreakpointObserver);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  readonly productosCart=inject(ProductCartService);
  private checkoutDraft = inject(AppointmentCheckoutDraftService);
  private readonly cart = inject(ReservationCartService);
  private queryHydrated = false;
  private availabilityVersion = 0;
  private quoteVersion = 0;
  readonly promocionId = signal<number | null>(null);
  readonly cotizacion = signal<CotizacionCitas | null>(null);
  readonly cotizando = signal(false);
  readonly errorCotizacion = signal('');
  readonly serviceSearch = signal('');
  selectionNotice = '';
  @ViewChild('dateSection') dateSection?: ElementRef<HTMLElement>;
  readonly tenantContext = inject(TenantContextService);
readonly diasDisponibles=signal<string[]>([]);
readonly diasMetadata=signal<import('../../services/appointment.service').DiasDisponibles|null>(null);
readonly cargandoDias=signal(false);
private diasVersion=0;
// La fecha del navegador solo posiciona la vista antes de cargar; nunca habilita reservas.
private get hoy(): Date { return this.diasMetadata() ? this.parsearFechaLocal(this.diasMetadata()!.hoyLocal) : this.normalizarFecha(new Date()); }
private get fechaMaximaReserva(): Date { return this.diasMetadata() ? this.parsearFechaLocal(this.diasMetadata()!.reservaHasta) : this.hoy; }
private readonly cargarDiasSeleccion=effect(() => {
  const servicios=this.selectedServices();
  this.prestadoresElegidos();
  untracked(() => this.cargarDiasDisponibles(servicios.map(s => s.id)));
});


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
  readonly prestadoresElegidos=signal<Record<number,number|null>>({});
  readonly errorStaff=signal('');
  readonly foliosReserva = signal<string[]>([]);
  readonly confirmacionExpiraEn=signal<string|null>(null);
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
  private readonly actualizarCotizacion = effect(() => {
    this.productosCart.items();
    const servicios = this.selectedServices();
    const itinerario = this.selectedItinerary();
    const fecha = this.selectedDate();
    const promocion = this.promocionId();
    untracked(() => this.cotizarReserva(servicios, itinerario, fecha, promocion));
  });
  errorMessage = '';
  readonly availableGroups = computed(() => {
    const groups = new Map<number, { id: number; nombre: string; total: number }>();
    for (const service of this.services()) {
      if (service.grupoId && service.grupoNombre) {
        const current = groups.get(service.grupoId);
        groups.set(service.grupoId, { id: service.grupoId, nombre: service.grupoNombre, total: (current?.total ?? 0) + 1 });
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
    const search = this.serviceSearch().trim().toLocaleLowerCase('es-MX');
    return this.services().filter(service =>
      (search || !this.selectedGroupId() || service.grupoId === this.selectedGroupId()) &&
      (search || !this.selectedSubgroupId() || service.subgrupoId === this.selectedSubgroupId()) &&
      (!search || (service.nombre + ' ' + service.descripcion).toLocaleLowerCase('es-MX').includes(search)));
  });
  readonly catalogContext = computed(() => {
    if (this.serviceSearch().trim()) return 'Resultados de búsqueda';
    const group = this.availableGroups().find(item => item.id === this.selectedGroupId())?.nombre;
    const subgroup = this.availableSubgroups().find(item => item.id === this.selectedSubgroupId())?.nombre;
    return [group, subgroup].filter(Boolean).join(' · ') || 'Todos los servicios';
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
      return this.diasDisponibles().includes(this.formatearFechaLocal(selectInfo.start));
    },
    datesSet: (arg) => { if(this.hasCatalogSelection()) this.cargarDiasDisponibles(this.selectedServices().map(s => s.id), arg.startStr.slice(0,10)); },
    select: (arg: DateSelectArg) => {
      this.onDateSelect(arg.startStr);
    }
  };

  ngOnInit(): void {
    const empresa=this.tenantContext.empresaId(); if(empresa) this.productosCart.connect(empresa);
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
    this.selectionNotice = this.selectedServices().length ? 'Cambiaste de sucursal. Elige los servicios disponibles en esta sede.' : '';
    this.services.set([]);
    this.selectedServices.set([]);
    this.saveCart();
    this.queryHydrated = true;
    this.serviceSearch.set('');
    this.staffByService.set({});
    this.prestadoresElegidos.set({});
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

  selectCatalogGroup(groupId: number | null): void {
    this.selectedGroupId.set(groupId);
    this.selectedSubgroupId.set(null);
    this.bookingForm.patchValue({ groupId, subgroupId: null });
  }

  selectCatalogSubgroup(subgroupId: number | null): void {
    this.selectedSubgroupId.set(subgroupId);
    this.bookingForm.patchValue({ subgroupId });
  }

  updateServiceSearch(value: string): void {
    this.serviceSearch.set(value);
    if (value.trim()) {
      this.selectedGroupId.set(null);
      this.selectedSubgroupId.set(null);
    }
  }

  serviceCategoryLabel(service: ServicioCatalogo): string {
    return [service.grupoNombre, service.subgrupoNombre].filter(Boolean).join(' · ');
  }

  onServiceChange(rawServiceId: string) {
    const serviceId = rawServiceId ? Number(rawServiceId) : null;
    this.bookingForm.patchValue({ serviceId });
    this.resetAvailabilityFlow();
  }

  toggleService(service: ServicioCatalogo) {
    if (this.isServiceSelected(service.id)) {
      this.removeSelectedService(service.id);
      return;
    }
    this.bookingForm.patchValue({ serviceId: service.id });
    this.addSelectedService();
  }

  isServiceSelected(id: number): boolean {
    return this.selectedServices().some(service => service.id === id);
  }

  private saveCart(): void {
    this.cart.save(this.tenantContext.empresaId(), Number(this.bookingForm.value.branchId) || null, this.selectedServices(), this.promocionId(), this.prestadoresElegidos());
  }

  private cotizarReserva(servicios: ServicioCatalogo[], itinerario: ItinerarioReserva | null,
      fecha: string | null, promocionId: number | null): void {
    const version = ++this.quoteVersion;
    this.cotizacion.set(null); this.errorCotizacion.set(''); this.cotizando.set(false);
    const empresaId = this.tenantContext.empresaId();
    const sucursalId = Number(this.bookingForm.value.branchId);
    if (!empresaId || !sucursalId || !servicios.length) return;
    this.cotizando.set(true);
    this.appointmentService.cotizarCitas({empresaId,sucursalId,promocionId,productos:this.productosCart.items().map(p=>({productoId:p.productoId,cantidad:p.cantidad})),
      items: servicios.map(servicio => ({servicioId:servicio.id, fecha,
        inicio: itinerario?.items.find(item => item.servicio.id===servicio.id)?.slot.inicio
          ?? null}))
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: resultado => { if(version!==this.quoteVersion) return; this.cotizacion.set(resultado); this.cotizando.set(false); },
      error: error => { if(version!==this.quoteVersion) return; this.cotizando.set(false); this.errorCotizacion.set(error?.error?.mensaje || error?.error?.message || 'No pudimos confirmar el precio. Intenta de nuevo antes de reservar.'); }
    });
  }

  reintentarCotizacion(): void {
    this.cotizarReserva(this.selectedServices(),this.selectedItinerary(),this.selectedDate(),this.promocionId());
  }

  continueBooking(): void {
    const target = this.selectedItinerary() ? this.dataSection : this.selectedDate() ? this.hoursSection : this.dateSection;
    target?.nativeElement.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' });
    target?.nativeElement.focus({ preventScroll: true });
  }

  cartActionLabel(): string {
    return this.selectedItinerary() ? 'Revisar y reservar' : this.selectedDate() ? 'Elegir mi horario' : 'Elegir mi fecha';
  }

  precioCotizado(servicioId: number) { return this.cotizacion()?.lineas.find(linea => linea.servicioId === servicioId); }

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

    if (this.selectedServices().some(item => (item.moneda || 'MXN') !== (service.moneda || 'MXN'))) {
      this.errorMessage = 'Los servicios de una misma reserva deben tener la misma moneda.';
      return;
    }
    this.selectedServices.update(actuales => [...actuales, service]);
    this.saveCart();
    this.cargarStaffServicio(service);
    this.resetAvailabilityFlow();
    this.errorMessage = '';
  }

  removeSelectedService(serviceId: number) {
    this.selectedServices.update(actuales => actuales.filter(service => service.id !== serviceId));
    this.bookingForm.patchValue({ serviceId: this.selectedServices()[0]?.id ?? null });
    this.saveCart();
    this.staffByService.update(actual => {
      const copia = { ...actual };
      delete copia[serviceId];
      return copia;
    });
    this.resetAvailabilityFlow();
  }

private cargarDiasDisponibles(servicios:number[],desde?:string): void {
  const version=++this.diasVersion;
  const sucursal=Number(this.bookingForm.value.branchId);
  if(!sucursal || !servicios.length) { this.diasDisponibles.set([]); this.cargandoDias.set(false); return; }
  this.cargandoDias.set(true);
  if(!desde) this.diasDisponibles.set([]);
  this.appointmentService.getAvailableDays(sucursal,servicios,desde,undefined,servicios.map(id=>this.prestadoresElegidos()[id]??null)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
    next: data => {
      if(version!==this.diasVersion) return;
      this.diasMetadata.set(data); this.diasDisponibles.update(actual => [...new Set([...actual,...data.dias])]);
      this.cargandoDias.set(false);
      if(!this.selectedDate() && data.dias.length) this.onDateSelect(data.dias[0]);
      else if(!this.selectedDate() && !data.dias.length) {
        const siguiente=this.sumarDias(this.parsearFechaLocal(desde ?? data.hoyLocal),31);
        if(this.formatearFechaLocal(siguiente)<=data.reservaHasta) this.cargarDiasDisponibles(servicios,this.formatearFechaLocal(siguiente));
        else this.errorMessage='No hay días disponibles en la ventana de reserva de este negocio.';
      }
    },
    error: () => { if(version===this.diasVersion) { this.cargandoDias.set(false); this.errorMessage='No pudimos cargar los días disponibles. Intenta de nuevo.'; } }
  });
}

  onDateSelect(date: string) {
    const empresaId = this.tenantContext.empresaId();
    if (!empresaId || !this.hasCatalogSelection()) {
      this.errorMessage = 'Selecciona primero una sucursal y un servicio.';
      return;
    }

    const version = ++this.availabilityVersion;
    this.selectedDate.set(date);
    this.sincronizarSemanaMovil(date);
    this.selectedHour.set(null);
    this.selectedItineraryId.set(null);
    this.loadingSlots.set(true);
    this.errorMessage = '';
    this.availableSlots.set([]);

    const requests = this.selectedServices().map(service => {
      const request: ConsultaFranjasRequest = {
        empresaId,
        sucursalId: Number(this.bookingForm.value.branchId),
        servicioId: service.id,
        prestadorId: this.prestadoresElegidos()[service.id] ?? null,
        fecha: date
      };
      return this.appointmentService.getAvailableSlots(request);
    });

    forkJoin(requests)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          if (version !== this.availabilityVersion) return;
          this.loadingSlots.set(false);
          setTimeout(() => { if (version === this.availabilityVersion) this.scrollToHours(); }, 100);
        })
      )
      .subscribe({
        next: (slotsByService) => {
          if (version !== this.availabilityVersion) return;
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
          if (version !== this.availabilityVersion) return;
          console.error('ERROR en la petición:', err);
          this.errorMessage = err?.message || 'No se pudieron cargar los horarios disponibles.';
          this.availableSlots.set([]);
          this.slotsByService.set({});
          this.selectedSeparatedSlots.set({});
          this.availableItineraries.set([]);
        }
      });
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
    this.cargarDiasDisponibles(this.selectedServices().map(s => s.id),this.formatearFechaLocal(propuesta));
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
    const empresaId = this.tenantContext.empresaId();
    if (!empresaId) { this.errorMessage = 'No se pudo identificar el negocio. Recarga la página para continuar.'; return; }
    if (this.cotizando() || !this.cotizacion()) { this.errorMessage = 'Espera a que confirmemos el precio de tu reserva.'; return; }
    if (this.cotizacion()?.reservableEnLinea === false) { this.errorMessage = this.cotizacion()?.motivoNoReservable || 'La reserva en línea no está disponible.'; return; }
    if (this.bookingForm.invalid || !this.selectedDate() || !itinerary || !this.selectedSlot) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    const pagarProductos=this.cotizacion()?.requierePagoProductos===true;
    this.isSubmitting.set(true);
    this.submitted.set(false);
    this.errorMessage = '';

    const phone = this.formatPhone(this.bookingForm.value.phone);

    const singleItem = itinerary.items.length === 1 ? itinerary.items[0] : null;
    const request$: Observable<CitaCreadaBackendResponse | CitasMultiplesCreadasResponse> = singleItem
      ? this.appointmentService.bookAppointment({
          empresaId,
          sucursalId: Number(this.bookingForm.value.branchId),
          promocionId: this.cotizacion()?.promocion?.id ?? null,
          servicioId: singleItem.servicio.id,
          prestadorId: singleItem.slot.prestadorId,
          nombreCliente: this.bookingForm.value.name.trim(),
          telefonoCliente: phone,
          correoCliente: this.bookingForm.value.email.trim(),
          inicio: singleItem.slot.inicio,
          notas: this.selectedServices().map(service => service.nombre).join(', ') || singleItem.servicio.nombre
        } as CrearCitaBackendRequest)
      : this.appointmentService.bookMultipleAppointments({
          empresaId,
          sucursalId: Number(this.bookingForm.value.branchId),
          promocionId: this.cotizacion()?.promocion?.id ?? null,
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
        next: async (response: any) => {
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

          this.foliosReserva.set((response?.id ? [response] : response?.citas ?? []).map((c: {folio?: string}) => c.folio).filter((f: unknown): f is string => typeof f === 'string' && !!f));
          this.confirmacionExpiraEn.set(response?.confirmacionExpiraEn ?? response?.citas?.[0]?.confirmacionExpiraEn ?? null);
          if (response?.id || response?.total) {
            const appointmentIds = response?.id
              ? (response.precio === 0 ? [] : [Number(response.id)])
              : (response?.citas ?? []).filter((cita: {precio?:number}) => cita.precio !== 0)
                  .map((cita: { id: number }) => Number(cita.id)).filter(Boolean);
            const requierePago=response?.requierePago===true || (response?.citas ?? []).some((cita:any)=>cita.requierePago===true);
            if ((requierePago && appointmentIds.length || pagarProductos) && await this.continuarAlPago(empresaId, requierePago?appointmentIds:[])) return;
            this.handleSuccess(response?.mensaje || 'Tu cita fue registrada correctamente.');
          } else {
            this.errorMessage = response?.mensaje || 'Hubo un problema al procesar la cita.';
          }
        },
        error: (err) => {
          console.error('Error al agendar cita:', err);
          this.errorMessage = err?.message || 'No se pudo completar la reserva.';
          if (err?.status === 409) this.reintentarCotizacion();
        }
      });
  }

  private async continuarAlPago(empresaId: number, appointmentIds: number[]): Promise<boolean> {
    const slug = this.tenantContext.slug();
    if (!slug) return false;
    try {
      this.checkoutDraft.save(empresaId, {
        appointmentIds,
        branchId: Number(this.bookingForm.value.branchId),
        name: this.bookingForm.value.name.trim(),
        email: this.bookingForm.value.email.trim().toLowerCase(),
        phone: this.formatPhone(this.bookingForm.value.phone)
      });
      await this.router.navigate([this.tenantContext.routeFor('checkout')], {
        queryParams: { citas: appointmentIds.join(',') }
      });
      return true;
    } catch {
      this.errorMessage = 'Tu reserva sigue pendiente de pago. No pudimos abrir el checkout; vuelve a abrir el pago para completarla.';
      return true;
    }
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
    this.cart.clear(this.tenantContext.empresaId());
    this.queryHydrated = true;
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
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (branches) => {
          this.loadingCatalog.set(false);
          this.branches.set(branches);
          const requestedBranch = Number(this.route.snapshot.queryParamMap.get('sucursal')) || this.cart.read(this.tenantContext.empresaId())?.branchId;
          const branch = branches.find(item => item.id === requestedBranch) ?? (branches.length === 1 ? branches[0] : null);
          if (branch) {
            this.bookingForm.patchValue({ branchId: branch.id });
            this.selectedGroupId.set(null);
            this.selectedSubgroupId.set(null);
            this.loadServices(branch.id);
          }
        },
        error: (err) => {
          this.loadingCatalog.set(false);
          console.error('Error cargando sucursales:', err);
          this.errorMessage = 'No se pudo cargar el catálogo de sucursales.';
        }
      });
  }

  private loadServices(branchId: number) {
    this.loadingCatalog.set(true);
    this.bookingDataService.getServices(branchId)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => { if (Number(this.bookingForm.value.branchId) === branchId) this.loadingCatalog.set(false); }))
      .subscribe({
        next: (services) => {
          if (Number(this.bookingForm.value.branchId) !== branchId) return;
          this.services.set(services);
          this.selectedGroupId.set(null);
          this.selectedSubgroupId.set(null);
          if (services.length === 1) {
            this.bookingForm.patchValue({ serviceId: services[0].id });
          } else {
            this.bookingForm.patchValue({ groupId: null, subgroupId: null, serviceId: null });
          }
          this.hidratarServiciosDesdeQuery();
          const initialService = this.selectedServices()[0];
          const initialGroupId = initialService?.grupoId ?? this.availableGroups()[0]?.id ?? null;
          this.selectCatalogGroup(initialGroupId);
        },
        error: (err) => {
          if (Number(this.bookingForm.value.branchId) !== branchId) return;
          console.error('Error cargando servicios:', err);
          this.errorMessage = 'No se pudo cargar el catálogo de servicios.';
        }
      });
  }

  private resetAvailabilityFlow() {
    this.availabilityVersion++;
    this.loadingSlots.set(false);
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
    return this.cotizacion()?.total ?? 0;
  }

  private sincronizarSemanaMovil(date: string) {
    this.mobileWeekStart.set(this.inicioSemana(this.parsearFechaLocal(date)));
  }

elegirPrestador(servicioId:number,prestadorId:number|null): void {
  this.resetAvailabilityFlow();
  this.prestadoresElegidos.update(actual=>({...actual,[servicioId]:prestadorId}));
  this.saveCart();
}
private cargarStaffServicio(service: ServicioCatalogo) {
  const empresaId=this.tenantContext.empresaId();
  const sucursalId=Number(this.bookingForm.value.branchId);
  if(!empresaId || !Number.isSafeInteger(sucursalId) || sucursalId<=0) return;
  this.appointmentService.getPublicStaff(empresaId,sucursalId,service.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
    next:staff=>{
      if(Number(this.bookingForm.value.branchId)!==sucursalId || !this.selectedServices().some(s=>s.id===service.id)) return;
      this.staffByService.update(actual=>({...actual,[service.id]:staff}));
      const elegido=this.prestadoresElegidos()[service.id];
      if(elegido && !staff.some(p=>p.usuarioId===elegido)) this.elegirPrestador(service.id,null);
    },
    error:()=>this.errorStaff.set('No pudimos cargar los profesionales. Puedes intentar de nuevo o elegir cualquier profesional disponible.')
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
      if (index >= selectedServices.length) {
        const inicio = items[0]?.slot.inicio ?? '';
        const fin = items[items.length - 1]?.slot.fin ?? '';
        const staffIds = new Set(items.map(item => item.slot.prestadorId).filter(Boolean));
        itineraries.push({
          id: items.map(item => `${item.servicio.id}-${item.slot.inicio}-${item.slot.prestadorId}`).join('|'),
          items: [...items],
          inicio,
          fin,
          totalPrecio: items.reduce((total, item) => total + item.servicio.precio, 0),
          totalMinutos: items.reduce((total, item) => {
            return total + item.servicio.duracionMinutos + item.servicio.bufferAntesMinutos + item.servicio.bufferDespuesMinutos;
          }, 0),
          usaMultiplesStaff: staffIds.size > 1
        });
        return true;
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
        const completed = walk(index + 1, slot.fin, items);
        items.pop();
        // Keep a complete route for every starting slot without expanding
        // every possible staff combination at the expense of later hours.
        if (completed && index > 0) return true;
      }
      return false;
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
    if (this.queryHydrated) return;
    this.queryHydrated = true;
    const query = this.route.snapshot.queryParamMap;
    const params = query.get('servicios') ?? query.get('servicio');
    const draft = this.cart.read(this.tenantContext.empresaId());
    const promo = query.has('promocion') ? Number(query.get('promocion')) : draft?.promocionId;
    this.prestadoresElegidos.set(params === null ? draft?.prestadores ?? {} : {});
    this.promocionId.set(promo && Number.isSafeInteger(promo) && promo>0 ? promo : null);
    const tokens = params !== null
      ? params.split(',').map(token => token.trim().toLowerCase()).filter(Boolean)
      : (draft?.serviceIds ?? []).map(String);
    const found = tokens.map(token => this.services().find(service =>
      String(service.id) === token || service.slug?.toLowerCase() === token)).filter((service): service is ServicioCatalogo => !!service);
    const unique = found.filter((service, index) => found.findIndex(item => item.id === service.id) === index);
    const services = unique.filter(service => (service.moneda || 'MXN') === (unique[0]?.moneda || 'MXN'));
    this.selectedServices.set(services);
    this.bookingForm.patchValue({ serviceId: services[0]?.id ?? null });
    if (services.length < tokens.length) this.selectionNotice = 'Algunos servicios ya no están disponibles para esta sucursal o moneda. Revisa tu selección.';
    services.forEach(service => this.cargarStaffServicio(service));
    this.saveCart();
  }

  private esFechaReservable(fecha: Date): boolean {
    return this.diasDisponibles().includes(this.formatearFechaLocal(fecha));
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
