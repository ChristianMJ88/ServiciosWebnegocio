import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { BookingDataService, CatalogoServiciosPublico, GrupoCatalogoPublico } from '../../services/booking-data.service';
import { ScrollRevealDirective } from '../../shared/directives/scroll-reveal.directive';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CommonModule, ScrollRevealDirective, MoneyDisplayPipe],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  readonly tenantContext = inject(TenantContextService);
  private readonly bookingDataService = inject(BookingDataService);
  readonly catalogo = signal<CatalogoServiciosPublico | null>(null);
  readonly cargandoServicios = signal(false);
  readonly tieneWhatsapp = computed(() => this.tenantContext.whatsappHabilitado());
  readonly heroCtaWhatsapp = computed(() =>
    this.tenantContext.heroAccionCta() === 'WHATSAPP' && this.tieneWhatsapp()
  );
  readonly gruposDestacados = computed(() => this.catalogo()?.grupos.slice(0, 3) ?? []);

  ngOnInit(): void {
    const slug = this.tenantContext.slug();
    if (!slug) {
      return;
    }

    this.cargandoServicios.set(true);
    this.bookingDataService.getPublicCatalogBySlug(slug).subscribe({
      next: catalogo => {
        this.catalogo.set(catalogo);
        this.cargandoServicios.set(false);
      },
      error: () => {
        this.catalogo.set(null);
        this.cargandoServicios.set(false);
      }
    });
  }

  imagenGrupo(grupo: GrupoCatalogoPublico): string {
    const imagenServicio = grupo.subgrupos.flatMap(subgrupo => subgrupo.servicios).find(servicio => !!servicio.imagenUrl)?.imagenUrl;
    return grupo.imagenUrl || imagenServicio || this.tenantContext.logoUrl() || '/fluora-mark.svg';
  }

  totalServiciosGrupo(grupo: GrupoCatalogoPublico): number {
    return grupo.subgrupos.reduce((total, subgrupo) => total + subgrupo.servicios.length, 0);
  }

  precioDesdeGrupo(grupo: GrupoCatalogoPublico): number | null {
    const precios = grupo.subgrupos.flatMap(subgrupo => subgrupo.servicios).map(servicio => servicio.precio);
    return precios.length ? Math.min(...precios) : null;
  }

  monedaGrupo(grupo: GrupoCatalogoPublico): string {
    return grupo.subgrupos.flatMap(subgrupo => subgrupo.servicios).find(servicio => !!servicio.moneda)?.moneda || 'MXN';
  }

  ordenSeccion(seccion: 'CONTACTO' | 'SERVICIOS'): number {
    const serviciosPrimero = this.tenantContext.inicioOrdenSecciones() === 'SERVICIOS_CONTACTO';
    return seccion === 'SERVICIOS'
      ? (serviciosPrimero ? 1 : 2)
      : (serviciosPrimero ? 2 : 1);
  }
}
