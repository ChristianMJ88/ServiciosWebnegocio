import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  AuditoriaConfiguracionAdmin,
  ConfiguracionMediaAdmin,
  ConfiguracionSitioAdmin,
  GuardarConfiguracionSitioPayload
} from '../../../core/admin/admin.service';
import { PlatformHostService } from '../../../core/platform/platform-host.service';
import { SiteBlockBuilderComponent } from './site-block-builder.component';
import { SiteFeaturedServicesComponent } from './site-featured-services.component';
import { SitePromotionsComponent } from './site-promotions.component';
import { SiteGalleryComponent } from './site-gallery.component';
import { SiteBenefitsComponent } from './site-benefits.component';
import { SiteReviewsComponent } from './site-reviews.component';
import { SiteFaqComponent } from './site-faq.component';
import { AdminSiteFacade } from './admin-site.facade';
import { finalize } from 'rxjs';

type SiteEditorSectionId =
  | 'estructura' | 'servicios_destacados' | 'beneficios' | 'promociones' | 'galeria'
  | 'resenas' | 'preguntas_frecuentes' | 'identidad' | 'apariencia'
  | 'navegacion' | 'portada' | 'contacto' | 'seo_footer' | 'publicacion';

@Component({
  selector: 'app-admin-site-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    SiteBlockBuilderComponent,
    SiteFeaturedServicesComponent,
    SitePromotionsComponent,
    SiteGalleryComponent,
    SiteBenefitsComponent,
    SiteReviewsComponent,
    SiteFaqComponent
  ],
  templateUrl: './admin-site-section.component.html',
  styleUrl: './admin-site-section.component.css'
})
export class AdminSiteSectionComponent implements OnInit {
  private readonly platformHost = inject(PlatformHostService);
  private readonly facade = inject(AdminSiteFacade);
  readonly defaultHeroImage = '/fluora-showcase-sites.svg';

  readonly formularioSitio = input.required<GuardarConfiguracionSitioPayload>();
  readonly guardandoSitio = input(false);
  readonly configuracionSitio = input<ConfiguracionSitioAdmin | null>(null);
  readonly auditoriaSitio = input<AuditoriaConfiguracionAdmin[]>([]);
  readonly etiquetaAccionAuditoriaConfiguracion = input.required<(accion: string) => string>();

  readonly save = output<void>();
  readonly seccionActiva = signal<SiteEditorSectionId>('estructura');
  readonly previewDevice = signal<'movil' | 'tablet' | 'escritorio'>('escritorio');
  readonly configuracionMedia = signal<ConfiguracionMediaAdmin | null>(null);
  readonly subiendoBranding = signal<'logo' | 'portada' | null>(null);
  readonly mensajeMedia = signal<string | null>(null);
  readonly errorMedia = signal<string | null>(null);
  readonly previewDimensions = computed(() => ({
    movil: '390 × 844 px',
    tablet: '820 × 1180 px',
    escritorio: '1440 × 900 px'
  })[this.previewDevice()]);

  private readonly seccionesBase: Array<{ id: SiteEditorSectionId; label: string; icon: string }> = [
    { id: 'estructura' as const, label: 'Estructura', icon: 'bi bi-layout-text-window-reverse' },
    { id: 'servicios_destacados' as const, label: 'Servicios destacados', icon: 'bi bi-stars' },
    { id: 'beneficios' as const, label: 'Beneficios', icon: 'bi bi-patch-check' },
    { id: 'promociones' as const, label: 'Promociones', icon: 'bi bi-tags' },
    { id: 'galeria' as const, label: 'Galería', icon: 'bi bi-images' },
    { id: 'resenas' as const, label: 'Reseñas', icon: 'bi bi-chat-quote' },
    { id: 'preguntas_frecuentes' as const, label: 'Preguntas frecuentes', icon: 'bi bi-question-circle' },
    { id: 'identidad' as const, label: 'Identidad', icon: 'bi bi-shop' },
    { id: 'apariencia' as const, label: 'Colores y tipografía', icon: 'bi bi-palette2' },
    { id: 'navegacion' as const, label: 'Menú', icon: 'bi bi-menu-button-wide' },
    { id: 'portada' as const, label: 'Portada', icon: 'bi bi-image' },
    { id: 'contacto' as const, label: 'Contacto y redes', icon: 'bi bi-chat-dots' },
    { id: 'seo_footer' as const, label: 'SEO y footer', icon: 'bi bi-search' },
    { id: 'publicacion' as const, label: 'Dominio y publicación', icon: 'bi bi-globe2' }
  ];

  readonly secciones = computed(() => [...this.seccionesBase]);

  readonly slugPreview = computed(() => this.normalizarSlug(
    this.formularioSitio().slug || this.formularioSitio().nombreComercial || 'mi-negocio'
  ));

  readonly rutaPublicaPreview = computed(() => this.platformHost.marketingUrl(`/e/${this.slugPreview()}`));

  readonly dominioPrincipalPreview = computed(() => {
    const dominio = this.normalizarDominio(this.formularioSitio().dominioPrincipal || '');
    return dominio ? `https://${dominio}` : null;
  });

  readonly heroPreviewImage = computed(() => this.formularioSitio().heroImagenUrl || this.defaultHeroImage);
  readonly heroPreviewVideo = computed(() => this.formularioSitio().heroVideoUrl?.trim() || null);

  readonly fuentesDisponibles = [
    { value: 'JAKARTA', label: 'Plus Jakarta Sans' },
    { value: 'INTER', label: 'Inter' },
    { value: 'PLAYFAIR', label: 'Playfair Display' },
    { value: 'MANROPE', label: 'Manrope' },
    { value: 'SYSTEM', label: 'Sistema' }
  ];

  readonly temasDisponibles = [
    { value: 'BASE', label: 'Base', description: 'Equilibrado y versátil' },
    { value: 'MINIMAL', label: 'Minimalista', description: 'Limpio y directo' },
    { value: 'ELEGANTE', label: 'Elegante', description: 'Suave y editorial' },
    { value: 'VIBRANTE', label: 'Vibrante', description: 'Color y energía' }
  ];

  readonly accionesDisponibles = [
    { value: 'AGENDAR', label: 'Abrir agenda' },
    { value: 'SERVICIOS', label: 'Ver servicios' },
    { value: 'CONTACTO', label: 'Ir a contacto' },
    { value: 'WHATSAPP', label: 'Abrir WhatsApp' }
  ];

  readonly accionesPortada = [
    ...this.accionesDisponibles,
    { value: 'EXTERNO', label: 'Abrir enlace externo' }
  ];

  ngOnInit(): void {
    this.facade.cargarConfiguracionMedia().subscribe({
      next: configuracion => this.configuracionMedia.set(configuracion),
      error: () => this.configuracionMedia.set(null)
    });
  }

  seleccionarBranding(evento: Event, destino: 'logo' | 'portada'): void {
    const inputArchivo = evento.target as HTMLInputElement;
    const archivo = inputArchivo.files?.[0];
    inputArchivo.value = '';
    if (!archivo || this.subiendoBranding() !== null) return;
    const configuracion = this.configuracionMedia();
    this.mensajeMedia.set(null);
    this.errorMedia.set(null);
    if (!configuracion?.cargaHabilitada) {
      this.errorMedia.set('Cloudinary todavía no está configurado. Puedes continuar usando una URL pública.');
      return;
    }
    if (!configuracion.tiposImagenPermitidos.includes(archivo.type)) {
      this.errorMedia.set('Usa una imagen JPG, PNG, WebP o AVIF.');
      return;
    }
    if (archivo.size > configuracion.maxBytesImagen) {
      this.errorMedia.set(`La imagen supera el máximo de ${this.megabytes(configuracion.maxBytesImagen)} MB.`);
      return;
    }
    this.subiendoBranding.set(destino);
    this.facade.subirImagenBranding(archivo).pipe(finalize(() => this.subiendoBranding.set(null))).subscribe({
      next: media => {
        if (destino === 'logo') this.formularioSitio().logoUrl = media.secureUrl;
        else this.formularioSitio().heroImagenUrl = media.secureUrl;
        this.mensajeMedia.set(`${destino === 'logo' ? 'Logo' : 'Imagen de portada'} cargado. Guarda los cambios para publicarlo.`);
      },
      error: err => this.errorMedia.set(err?.error?.message || err?.error?.detail || 'No fue posible cargar la imagen.')
    });
  }

  megabytes(bytes: number): string {
    return (bytes / 1024 / 1024).toFixed(bytes % (1024 * 1024) === 0 ? 0 : 1);
  }

  fuenteCss(valor?: string | null): string {
    const fuentes: Record<string, string> = {
      JAKARTA: '"Plus Jakarta Sans", Inter, sans-serif',
      INTER: 'Inter, "Segoe UI", sans-serif',
      PLAYFAIR: '"Playfair Display", Georgia, serif',
      MANROPE: 'Manrope, Inter, sans-serif',
      SYSTEM: 'system-ui, -apple-system, "Segoe UI", sans-serif'
    };
    return fuentes[valor || ''] || fuentes['INTER'];
  }

  private normalizarSlug(valor: string): string {
    const slug = valor
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');

    return slug || 'mi-negocio';
  }

  private normalizarDominio(valor: string): string {
    return valor
      .trim()
      .toLowerCase()
      .replace(/^https?:\/\//, '')
      .replace(/\/.*$/, '');
  }
}
