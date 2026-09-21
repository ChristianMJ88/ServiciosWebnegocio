import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { TenantFeaturedServiceItem, TenantGalleryImage, TenantPromotion } from '../../core/tenant/tenant.types';
import { BookingDataService, CatalogoServiciosPublico, GrupoCatalogoPublico } from '../../services/booking-data.service';
import { ScrollRevealDirective } from '../../shared/directives/scroll-reveal.directive';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
import { PublicSectionHeadingComponent } from '../../shared/ui/public-section-heading/public-section-heading.component';
import { PublicTeamCardComponent } from '../../shared/ui/public-team-card/public-team-card.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CommonModule, ScrollRevealDirective, MoneyDisplayPipe, PublicSectionHeadingComponent, PublicTeamCardComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  readonly tenantContext = inject(TenantContextService);
  private readonly bookingDataService = inject(BookingDataService);
  readonly catalogo = signal<CatalogoServiciosPublico | null>(null);
  readonly cargandoServicios = signal(false);
  readonly imagenGaleriaActiva = signal<TenantGalleryImage | null>(null);
  readonly tieneWhatsapp = computed(() => this.tenantContext.whatsappHabilitado());
  readonly destacados = computed<TenantFeaturedServiceItem[]>(() => {
    const configuracion = this.tenantContext.serviciosDestacados();
    if (configuracion) return configuracion.elementos;

    return (this.catalogo()?.grupos.slice(0, 3) ?? []).map(grupo => ({
      tipo: 'GRUPO',
      referenciaId: grupo.id,
      grupoId: grupo.id,
      subgrupoId: null,
      servicioId: null,
      titulo: grupo.nombre,
      descripcion: grupo.descripcion,
      imagenUrl: this.imagenGrupo(grupo),
      precioDesde: this.precioDesdeGrupo(grupo),
      moneda: this.monedaGrupo(grupo),
      duracionMinutos: null,
      cantidadServicios: this.totalServiciosGrupo(grupo),
      etiqueta: null
    }));
  });
  readonly vistaDestacados = computed(() => this.tenantContext.serviciosDestacados()?.vista ?? 'TARJETAS');
  readonly mostrarPrecioDestacados = computed(() => this.tenantContext.serviciosDestacados()?.mostrarPrecio ?? true);
  readonly mostrarDuracionDestacados = computed(() => this.tenantContext.serviciosDestacados()?.mostrarDuracion ?? true);
  readonly mostrarDescripcionDestacados = computed(() => this.tenantContext.serviciosDestacados()?.mostrarDescripcion ?? true);
  private readonly iconosBeneficios: Record<string, string> = {
    ESTRELLA: 'bi bi-star', CORAZON: 'bi bi-heart', ESCUDO: 'bi bi-shield-check', DESTELLO: 'bi bi-stars',
    RELOJ: 'bi bi-clock', PERSONA: 'bi bi-person-heart', HOJA: 'bi bi-flower1', PREMIO: 'bi bi-award'
  };

  ngOnInit(): void {
    const slug = this.tenantContext.slug();
    if (!slug) {
      return;
    }

    if (this.tenantContext.serviciosDestacados()) return;

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

  imagenDestacado(item: TenantFeaturedServiceItem): string {
    return item.imagenUrl || this.tenantContext.logoUrl() || '/fluora-mark.svg';
  }

  imagenPromocion(item: TenantPromotion): string {
    return item.imagenUrl || this.tenantContext.heroImagenUrl();
  }

  parametrosPromocion(item: TenantPromotion): { servicios: string } | null {
    return item.servicioIds.length ? { servicios: item.servicioIds.join(',') } : null;
  }

  iconoBeneficio(icono: string): string {
    return this.iconosBeneficios[icono] ?? 'bi bi-stars';
  }

  numeroBeneficio(indice: number): string {
    return String(indice + 1).padStart(2, '0');
  }

  abrirImagenGaleria(item: TenantGalleryImage): void {
    if (this.tenantContext.galeria()?.ampliacionHabilitada) this.imagenGaleriaActiva.set(item);
  }

  srcsetGaleria(item: TenantGalleryImage): string | null {
    const variantes = [
      item.imagenUrlPequena ? `${item.imagenUrlPequena} 480w` : null,
      item.imagenUrlMediana ? `${item.imagenUrlMediana} 960w` : null,
      item.imagenUrlGrande ? `${item.imagenUrlGrande} 1440w` : null
    ].filter((url): url is string => !!url);
    return variantes.length ? variantes.join(', ') : null;
  }

  cerrarImagenGaleria(): void {
    this.imagenGaleriaActiva.set(null);
  }

  @HostListener('document:keydown.escape')
  cerrarGaleriaConEscape(): void {
    this.cerrarImagenGaleria();
  }

}
