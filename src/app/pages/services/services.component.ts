import { Component, DestroyRef, ElementRef, ViewChild, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { AppointmentService, PrestadorPublico } from '../../services/appointment.service';
import {
  BookingDataService,
  CatalogoServiciosPublico,
  GrupoCatalogoPublico,
  ServicioCatalogo,
  SucursalCatalogo,
  SubgrupoCatalogoPublico
} from '../../services/booking-data.service';
import { ReservationCartService } from '../../services/reservation-cart.service';
import { ReservationCartComponent } from '../../shared/components/reservation-cart/reservation-cart.component';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { ScrollRevealDirective } from '../../shared/directives/scroll-reveal.directive';

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule, RouterLink, ScrollRevealDirective, FormsModule, ReservationCartComponent, MoneyDisplayPipe],
  templateUrl: './services.component.html',
  styleUrls: ['./services.component.css']
})
export class ServicesComponent implements OnInit {
  readonly tenantContext = inject(TenantContextService);
  private readonly bookingDataService = inject(BookingDataService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cart = inject(ReservationCartService);
  private readonly router = inject(Router);
  @ViewChild('serviceDetails') private serviceDetails?: ElementRef<HTMLDialogElement>;
  readonly branches = signal<SucursalCatalogo[]>([]);
  readonly branchId = signal<number | null>(null);
  readonly search = signal('');
  readonly notice = signal('');
  readonly selectedBranch = computed(() => this.branches().find(branch => branch.id === this.branchId()));
  readonly catalogo = signal<CatalogoServiciosPublico | null>(null);
  readonly cargando = signal(false);
  readonly grupoActivoId = signal<number | null>(null);
  readonly subgrupoActivoId = signal<number | null>(null);
  readonly servicioSeleccionadoId = signal<number | null>(null);
  readonly serviciosSeleccionados = signal<ServicioCatalogo[]>([]);
  readonly staffServicio = signal<PrestadorPublico[]>([]);
  readonly cargandoStaff = signal(false);
  readonly grupos = computed(() => (this.catalogo()?.grupos ?? []).map(group => ({ ...group,
    subgrupos: group.subgrupos.map(subgroup => ({ ...subgroup, servicios: subgroup.servicios.filter(service =>
      (!this.branchId() || service.sucursalId === this.branchId()) &&
      (!this.search().trim() || (service.nombre + ' ' + service.descripcion).toLocaleLowerCase('es-MX').includes(this.search().trim().toLocaleLowerCase('es-MX'))))
    })).filter(subgroup => subgroup.servicios.length)
  })).filter(group => group.subgrupos.length));
  readonly serviciosCatalogo = computed(() =>
    this.grupos().flatMap(grupo => grupo.subgrupos).flatMap(subgrupo => subgrupo.servicios)
  );
  readonly totalServicios = computed(() =>
    this.grupos().reduce(
      (total, grupo) => total + grupo.subgrupos.reduce((subtotal, subgrupo) => subtotal + subgrupo.servicios.length, 0),
      0
    )
  );
  readonly precioDesde = computed(() => {
    const precios = this.grupos()
      .flatMap(grupo => grupo.subgrupos)
      .flatMap(subgrupo => subgrupo.servicios)
      .map(servicio => servicio.precio)
      .filter(precio => Number.isFinite(precio));
    return precios.length ? Math.min(...precios) : null;
  });
  readonly gruposVisibles = computed(() => {
    const grupoActivoId = this.grupoActivoId();
    return grupoActivoId
      ? this.grupos().filter(grupo => grupo.id === grupoActivoId)
      : this.grupos();
  });
  readonly subgruposDisponibles = computed(() => {
    const grupoActivoId = this.grupoActivoId();
    if (!grupoActivoId) {
      return [];
    }
    return this.grupos().find(grupo => grupo.id === grupoActivoId)?.subgrupos ?? [];
  });
  readonly servicioSeleccionado = computed(() => {
    const servicioId = this.servicioSeleccionadoId();
    if (!servicioId) {
      return null;
    }
    return this.serviciosCatalogo().find(servicio => servicio.id === servicioId) ?? null;
  });
  readonly totalSeleccionado = computed(() =>
    this.serviciosSeleccionados().reduce((total, servicio) => total + servicio.precio, 0)
  );
  readonly duracionSeleccionada = computed(() =>
    this.serviciosSeleccionados().reduce(
      (total, servicio) => total + servicio.duracionMinutos + servicio.bufferAntesMinutos + servicio.bufferDespuesMinutos,
      0
    )
  );

  ngOnInit(): void {
    const slug = this.tenantContext.slug();
    if (!slug) {
      return;
    }

    this.cargando.set(true);
    forkJoin({ catalogo: this.bookingDataService.getPublicCatalogBySlug(slug), branches: this.bookingDataService.getBranches() }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ catalogo, branches }) => {
        this.catalogo.set(catalogo);
        this.branches.set(branches);
        const draft = this.cart.read(this.tenantContext.empresaId());
        const branch = branches.find(item => item.id === draft?.branchId) ?? (branches.length === 1 ? branches[0] : null);
        this.branchId.set(branch?.id ?? null);
        const services = catalogo.grupos.flatMap(group => group.subgrupos).flatMap(group => group.servicios);
        this.serviciosSeleccionados.set((draft?.serviceIds ?? []).map(id => services.find(service => service.id === id && service.sucursalId === branch?.id)).filter((service): service is ServicioCatalogo => !!service));
        if (this.serviciosSeleccionados().length < (draft?.serviceIds.length ?? 0)) this.notice.set('El catálogo cambió. Revisa los servicios disponibles para esta sucursal.');
        this.saveCart();
        this.sincronizarServicioSeleccionado();
        this.cargando.set(false);
      },
      error: () => {
        this.catalogo.set(null);
        this.cargando.set(false);
      }
    });
  }

  seleccionarGrupo(grupoId: number | null) {
    this.grupoActivoId.set(grupoId);
    if (grupoId === null) {
      this.subgrupoActivoId.set(null);
      this.sincronizarServicioSeleccionado();
      return;
    }

    const subgrupos = this.grupos().find(grupo => grupo.id === grupoId)?.subgrupos ?? [];
    if (!subgrupos.some(subgrupo => subgrupo.id === this.subgrupoActivoId())) {
      this.subgrupoActivoId.set(null);
    }
    this.sincronizarServicioSeleccionado();
  }

  seleccionarSubgrupo(subgrupoId: number | null) {
    this.subgrupoActivoId.set(subgrupoId);
    this.sincronizarServicioSeleccionado();
  }

  openDetails(servicio: ServicioCatalogo): void {
    this.seleccionarServicio(servicio);
    this.serviceDetails?.nativeElement.showModal();
  }

  closeDetails(): void { this.serviceDetails?.nativeElement.close(); }

  seleccionarServicio(servicio: ServicioCatalogo) {
    this.servicioSeleccionadoId.set(servicio.id);
    this.cargarStaffServicio(servicio);
  }

  agregarServicio(servicio: ServicioCatalogo) {
    if (!this.branchId() || servicio.sucursalId !== this.branchId()) return;
    this.seleccionarServicio(servicio);
    if (this.servicioEstaSeleccionado(servicio.id)) {
      this.quitarServicio(servicio.id);
      return;
    }
    if (this.serviciosSeleccionados().some(item => (item.moneda || 'MXN') !== (servicio.moneda || 'MXN'))) {
      this.notice.set('Los servicios de una misma reserva deben tener la misma moneda.');
      return;
    }
    this.serviciosSeleccionados.update(actuales => [...actuales, servicio]);
    this.notice.set('');
    this.saveCart();
  }

  quitarServicio(servicioId: number) {
    this.serviciosSeleccionados.update(actuales => actuales.filter(servicio => servicio.id !== servicioId));
    this.saveCart();
  }

  servicioEstaSeleccionado(servicioId: number): boolean {
    return this.serviciosSeleccionados().some(servicio => servicio.id === servicioId);
  }

  changeBranch(value: string): void {
    this.branchId.set(Number(value) || null);
    const hadServices = this.serviciosSeleccionados().length > 0;
    this.serviciosSeleccionados.set([]);
    this.grupoActivoId.set(null);
    this.subgrupoActivoId.set(null);
    this.saveCart();
    this.sincronizarServicioSeleccionado();
    this.notice.set(hadServices ? 'Cambiaste de sucursal. Agrega los servicios disponibles en esta sede.' : '');
  }

  private saveCart(): void {
    this.cart.save(this.tenantContext.empresaId(), this.branchId(), this.serviciosSeleccionados());
  }

  continueBooking(): void {
    if (!this.serviciosSeleccionados().length) return;
    this.saveCart();
    void this.router.navigate([this.tenantContext.routeFor('agendar')], { queryParams: this.bookingQueryParams() });
  }

  bookingQueryParams(): Record<string, string> {
    return { sucursal: String(this.branchId() || ''), servicios: this.serviciosSeleccionados().map(service => String(service.id)).join(',') };
  }

  catalogCurrency(services: ServicioCatalogo[]): string {
    return services[0]?.moneda || 'MXN';
  }

  groupServices(group: GrupoCatalogoPublico): ServicioCatalogo[] {
    return group.subgrupos.flatMap(subgroup => subgroup.servicios);
  }

  sameCurrency(services: ServicioCatalogo[]): boolean {
    return new Set(services.map(service => service.moneda || 'MXN')).size <= 1;
  }

  imagenServicio(grupo: GrupoCatalogoPublico, servicio: ServicioCatalogo): string {
    return servicio.imagenUrl || grupo.imagenUrl || this.tenantContext.logoUrl() || '/fluora-mark.svg';
  }

  subgruposVisibles(grupo: GrupoCatalogoPublico): SubgrupoCatalogoPublico[] {
    const subgrupoActivoId = this.subgrupoActivoId();
    if (!subgrupoActivoId) {
      return grupo.subgrupos;
    }
    return grupo.subgrupos.filter(subgrupo => subgrupo.id === subgrupoActivoId);
  }

  totalServiciosGrupo(grupo: GrupoCatalogoPublico): number {
    return grupo.subgrupos.reduce((total, subgrupo) => total + subgrupo.servicios.length, 0);
  }

  precioDesdeGrupo(grupo: GrupoCatalogoPublico): number | null {
    const precios = grupo.subgrupos.flatMap(subgrupo => subgrupo.servicios).map(servicio => servicio.precio);
    return precios.length ? Math.min(...precios) : null;
  }

  whatsappServicioHref(): string {
    const servicios = this.serviciosSeleccionados();
    const seleccion = servicios.length
      ? servicios.map(servicio => servicio.nombre)
      : this.servicioSeleccionado()
        ? [this.servicioSeleccionado()!.nombre]
        : [];

    if (!seleccion.length) {
      return this.tenantContext.whatsappHref();
    }

    return this.tenantContext.whatsappHref(
      `${this.tenantContext.whatsappMensaje()}\nServicios seleccionados: ${seleccion.join(', ')}.`
    );
  }

  private sincronizarServicioSeleccionado() {
    const serviciosVisibles = this.gruposVisibles()
      .flatMap(grupo => this.subgruposVisibles(grupo))
      .flatMap(subgrupo => subgrupo.servicios);
    const servicioActual = this.servicioSeleccionadoId();
    if (servicioActual && serviciosVisibles.some(servicio => servicio.id === servicioActual)) {
      return;
    }
    const servicio = serviciosVisibles[0] ?? null;
    this.servicioSeleccionadoId.set(servicio?.id ?? null);
    if (servicio) {
      this.cargarStaffServicio(servicio);
    }
  }

  private cargarStaffServicio(servicio: ServicioCatalogo) {
    const empresaId = this.catalogo()?.empresaId ?? this.tenantContext.empresaId();
    if (!empresaId || !servicio.sucursalId) {
      this.staffServicio.set([]);
      return;
    }

    this.staffServicio.set([]);
    this.cargandoStaff.set(true);
    this.appointmentService.getPublicStaff(empresaId, servicio.sucursalId, servicio.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: staff => {
        if (this.servicioSeleccionadoId() !== servicio.id) return;
        this.staffServicio.set(staff);
        this.cargandoStaff.set(false);
      },
      error: () => {
        if (this.servicioSeleccionadoId() !== servicio.id) return;
        this.staffServicio.set([]);
        this.cargandoStaff.set(false);
      }
    });
  }
}
