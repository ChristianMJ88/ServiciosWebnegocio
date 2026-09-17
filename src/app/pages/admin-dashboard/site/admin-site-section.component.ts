import { CommonModule } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import {
  AuditoriaConfiguracionAdmin,
  ConfiguracionSitioAdmin,
  GuardarConfiguracionSitioPayload
} from '../../../core/admin/admin.service';
import { PlatformHostService } from '../../../core/platform/platform-host.service';

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
    MatSlideToggleModule
  ],
  templateUrl: './admin-site-section.component.html',
  styleUrl: './admin-site-section.component.css'
})
export class AdminSiteSectionComponent {
  private readonly platformHost = inject(PlatformHostService);
  readonly defaultHeroImage = '/fluora-showcase-sites.svg';

  readonly formularioSitio = input.required<GuardarConfiguracionSitioPayload>();
  readonly guardandoSitio = input(false);
  readonly configuracionSitio = input<ConfiguracionSitioAdmin | null>(null);
  readonly auditoriaSitio = input<AuditoriaConfiguracionAdmin[]>([]);
  readonly etiquetaAccionAuditoriaConfiguracion = input.required<(accion: string) => string>();

  readonly save = output<void>();
  readonly seccionActiva = signal<'identidad' | 'apariencia' | 'navegacion' | 'portada' | 'contacto' | 'seo_footer' | 'publicacion'>('identidad');
  readonly previewDevice = signal<'movil' | 'tablet' | 'escritorio'>('escritorio');
  readonly previewDimensions = computed(() => ({
    movil: '390 × 844 px',
    tablet: '820 × 1180 px',
    escritorio: '1440 × 900 px'
  })[this.previewDevice()]);

  readonly secciones = [
    { id: 'identidad' as const, label: 'Identidad', icon: 'bi bi-shop' },
    { id: 'apariencia' as const, label: 'Colores y tipografía', icon: 'bi bi-palette2' },
    { id: 'navegacion' as const, label: 'Menú', icon: 'bi bi-menu-button-wide' },
    { id: 'portada' as const, label: 'Portada', icon: 'bi bi-image' },
    { id: 'contacto' as const, label: 'Contacto y redes', icon: 'bi bi-chat-dots' },
    { id: 'seo_footer' as const, label: 'SEO y footer', icon: 'bi bi-search' },
    { id: 'publicacion' as const, label: 'Dominio y publicación', icon: 'bi bi-globe2' }
  ];

  readonly slugPreview = computed(() => this.normalizarSlug(
    this.formularioSitio().slug || this.formularioSitio().nombreComercial || 'mi-negocio'
  ));

  readonly rutaPublicaPreview = computed(() => this.platformHost.marketingUrl(`/e/${this.slugPreview()}`));

  readonly dominioPrincipalPreview = computed(() => {
    const dominio = this.normalizarDominio(this.formularioSitio().dominioPrincipal || '');
    return dominio ? `https://${dominio}` : null;
  });

  readonly heroPreviewImage = computed(() => this.formularioSitio().heroImagenUrl || this.defaultHeroImage);

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
