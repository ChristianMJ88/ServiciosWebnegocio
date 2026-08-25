import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { BookingDataService, CatalogoServiciosPublico, GrupoCatalogoPublico } from '../../services/booking-data.service';
import { ScrollRevealDirective } from '../../shared/directives/scroll-reveal.directive';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CommonModule, ScrollRevealDirective],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  readonly tenantContext = inject(TenantContextService);
  private readonly bookingDataService = inject(BookingDataService);
  readonly catalogo = signal<CatalogoServiciosPublico | null>(null);
  readonly cargandoServicios = signal(false);
  readonly tieneWhatsapp = computed(() => !!this.tenantContext.whatsapp());
  readonly gruposDestacados = computed(() => this.catalogo()?.grupos.slice(0, 3) ?? []);
  readonly imagenesServicioFallback = [
    'https://images.unsplash.com/photo-1562322140-8baeececf3df?auto=format&fit=crop&w=1200&q=80',
    'https://images.unsplash.com/photo-1521590832167-7bcbfaa6381f?auto=format&fit=crop&w=1200&q=80',
    'https://images.unsplash.com/photo-1487412947147-5cebf100ffc2?auto=format&fit=crop&w=1200&q=80'
  ];

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

  imagenGrupo(grupo: GrupoCatalogoPublico, index: number): string {
    const imagenServicio = grupo.subgrupos.flatMap(subgrupo => subgrupo.servicios).find(servicio => !!servicio.imagenUrl)?.imagenUrl;
    return grupo.imagenUrl || imagenServicio || this.imagenesServicioFallback[index % this.imagenesServicioFallback.length];
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

  mensajeWhatsappHero(): string {
    return `Hola, quiero reservar una cita en ${this.tenantContext.nombreComercial()}.`;
  }
}
