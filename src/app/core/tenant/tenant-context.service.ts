import { Injectable, computed, signal } from '@angular/core';
import { TenantContext, TenantSiteConfig } from './tenant.types';

@Injectable({
  providedIn: 'root'
})
export class TenantContextService {
  private readonly tenant = signal<TenantContext | null>(null);

  readonly actual = computed(() => this.tenant());
  readonly activo = computed(() => Boolean(this.tenant()));
  readonly slug = computed(() => this.tenant()?.slug ?? null);
  readonly empresaId = computed(() => this.tenant()?.empresaId ?? null);
  readonly dominioPrincipal = computed(() => this.tenant()?.dominioPrincipal ?? null);
  readonly nombreComercial = computed(() => this.tenant()?.nombreComercial ?? 'Fluora');
  readonly descripcionCorta = computed(() =>
    this.tenant()?.descripcionCorta ?? 'Software multitenant para servicios, agenda y operación.'
  );
  readonly colorPrimario = computed(() => this.tenant()?.colorPrimario ?? '#2563eb');
  readonly colorSecundario = computed(() => this.tenant()?.colorSecundario ?? '#0f766e');
  readonly fuenteTitulos = computed(() => this.tenant()?.fuenteTitulos ?? 'JAKARTA');
  readonly fuenteCuerpo = computed(() => this.tenant()?.fuenteCuerpo ?? 'INTER');
  readonly heroTitulo = computed(() => this.tenant()?.heroTitulo ?? this.nombreComercial());
  readonly heroSubtitulo = computed(() => this.tenant()?.heroSubtitulo ?? this.descripcionCorta());
  readonly heroImagenUrl = computed(() => this.tenant()?.heroImagenUrl ?? '/tenant-hero-demo.png');
  readonly logoUrl = computed(() => this.tenant()?.logoUrl ?? null);
  readonly whatsapp = computed(() => this.tenant()?.whatsapp ?? null);
  readonly telefono = computed(() => this.tenant()?.telefono ?? this.tenant()?.whatsapp ?? '');
  readonly correo = computed(() => this.tenant()?.correo ?? '');
  readonly direccion = computed(() => this.tenant()?.direccion ?? '');
  readonly instagramUrl = computed(() => this.tenant()?.instagramUrl ?? '');
  readonly facebookUrl = computed(() => this.tenant()?.facebookUrl ?? '');

  setTenant(config: TenantSiteConfig) {
    this.tenant.set({ ...config });
  }

  clearTenant() {
    this.tenant.set(null);
  }

  routeFor(segment = ''): string {
    const slug = this.slug();
    if (!slug) {
      return segment ? `/${segment}` : '/';
    }

    return segment ? `/e/${slug}/${segment}` : `/e/${slug}`;
  }

  whatsappHref(message = 'Hola quiero agendar'): string {
    const whatsapp = this.whatsapp();
    if (!whatsapp) {
      return '#';
    }

    return `https://wa.me/${whatsapp}?text=${encodeURIComponent(message)}`;
  }

  telefonoHref(): string {
    const telefono = this.telefono().replace(/\s+/g, '');
    return telefono ? `tel:+${telefono.replace(/^\+/, '')}` : '#';
  }

  correoHref(): string {
    const correo = this.correo();
    return correo ? `mailto:${correo}` : '#';
  }

  initials(): string {
    return this.nombreComercial()
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(fragment => fragment.charAt(0).toUpperCase())
      .join('') || 'FL';
  }
}
