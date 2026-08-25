import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { AppointmentService, PrestadorPublico } from '../../services/appointment.service';
import {
  BookingDataService,
  CatalogoServiciosPublico,
  GrupoCatalogoPublico,
  ServicioCatalogo,
  SubgrupoCatalogoPublico
} from '../../services/booking-data.service';
import { ScrollRevealDirective } from '../../shared/directives/scroll-reveal.directive';

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [CommonModule, RouterLink, ScrollRevealDirective],
  templateUrl: './services.component.html',
  styleUrls: ['./services.component.css']
})
export class ServicesComponent implements OnInit {
  readonly tenantContext = inject(TenantContextService);
  private readonly bookingDataService = inject(BookingDataService);
  private readonly appointmentService = inject(AppointmentService);
  readonly catalogo = signal<CatalogoServiciosPublico | null>(null);
  readonly cargando = signal(false);
  readonly grupoActivoId = signal<number | null>(null);
  readonly subgrupoActivoId = signal<number | null>(null);
  readonly servicioSeleccionadoId = signal<number | null>(null);
  readonly serviciosSeleccionados = signal<ServicioCatalogo[]>([]);
  readonly staffServicio = signal<PrestadorPublico[]>([]);
  readonly cargandoStaff = signal(false);
  readonly grupos = computed(() => this.catalogo()?.grupos ?? []);
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
    this.bookingDataService.getPublicCatalogBySlug(slug).subscribe({
      next: catalogo => {
        this.catalogo.set(catalogo);
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

  seleccionarServicio(servicio: ServicioCatalogo) {
    this.servicioSeleccionadoId.set(servicio.id);
    this.cargarStaffServicio(servicio);
  }

  agregarServicio(servicio: ServicioCatalogo) {
    this.seleccionarServicio(servicio);
    if (this.servicioEstaSeleccionado(servicio.id)) {
      return;
    }
    this.serviciosSeleccionados.update(actuales => [...actuales, servicio]);
  }

  quitarServicio(servicioId: number) {
    this.serviciosSeleccionados.update(actuales => actuales.filter(servicio => servicio.id !== servicioId));
  }

  servicioEstaSeleccionado(servicioId: number): boolean {
    return this.serviciosSeleccionados().some(servicio => servicio.id === servicioId);
  }

  bookingQueryParams(): Record<string, string> {
    const servicios = this.serviciosSeleccionados();
    if (!servicios.length && this.servicioSeleccionado()) {
      const servicio = this.servicioSeleccionado()!;
      return { servicio: servicio.slug || String(servicio.id) };
    }

    return {
      servicios: servicios.map(servicio => servicio.slug || String(servicio.id)).join(','),
      servicio: servicios[0]?.slug || String(servicios[0]?.id || '')
    };
  }

  imagenServicio(grupo: GrupoCatalogoPublico, servicio: ServicioCatalogo): string {
    return servicio.imagenUrl || grupo.imagenUrl || this.tenantContext.logoUrl() || '/NailArt_logo.jpeg';
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
    const negocio = this.catalogo()?.nombreComercial || this.tenantContext.nombreComercial();
    const servicios = this.serviciosSeleccionados();
    const mensaje = servicios.length
      ? `Hola, quiero reservar ${servicios.map(servicio => servicio.nombre).join(', ')} en ${negocio}.`
      : this.servicioSeleccionado()
        ? `Hola, quiero reservar ${this.servicioSeleccionado()!.nombre} en ${negocio}.`
      : `Hola, quiero reservar una cita en ${negocio}.`;
    return this.tenantContext.whatsappHref(mensaje);
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

    this.cargandoStaff.set(true);
    this.appointmentService.getPublicStaff(empresaId, servicio.sucursalId, servicio.id).subscribe({
      next: staff => {
        this.staffServicio.set(staff);
        this.cargandoStaff.set(false);
      },
      error: () => {
        this.staffServicio.set([]);
        this.cargandoStaff.set(false);
      }
    });
  }
}
