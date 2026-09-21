import { Injectable, computed, signal } from '@angular/core';
import { TenantContext, TenantSiteBlock, TenantSiteConfig } from './tenant.types';

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
    this.tenant()?.descripcionCorta ?? 'Agenda, clientes y operación en un solo lugar.'
  );
  readonly colorPrimario = computed(() => this.tenant()?.colorPrimario ?? '#2563eb');
  readonly colorSecundario = computed(() => this.tenant()?.colorSecundario ?? '#0f766e');
  readonly fuenteTitulos = computed(() => this.tenant()?.fuenteTitulos ?? 'JAKARTA');
  readonly fuenteCuerpo = computed(() => this.tenant()?.fuenteCuerpo ?? 'INTER');
  readonly heroTitulo = computed(() => this.tenant()?.heroTitulo ?? this.nombreComercial());
  readonly heroSubtitulo = computed(() => this.tenant()?.heroSubtitulo ?? this.descripcionCorta());
  readonly heroImagenUrl = computed(() => this.tenant()?.heroImagenUrl ?? '/fluora-showcase-sites.svg');
  readonly heroTipoMedio = computed(() => this.option(this.tenant()?.heroTipoMedio, ['IMAGEN', 'VIDEO'], 'IMAGEN'));
  readonly heroVideoUrl = computed(() => this.safeExternalUrl(this.tenant()?.heroVideoUrl));
  readonly heroAltura = computed(() => this.option(this.tenant()?.heroAltura, ['COMPACTA', 'MEDIA', 'PANTALLA'], 'PANTALLA'));
  readonly heroEtiqueta = computed(() => this.tenant()?.heroEtiqueta ?? '');
  readonly heroAlineacion = computed(() => this.option(this.tenant()?.heroAlineacion, ['IZQUIERDA', 'CENTRO'], 'IZQUIERDA'));
  readonly heroOverlay = computed(() => this.option(this.tenant()?.heroOverlay, ['SUAVE', 'MEDIO', 'ALTO'], 'MEDIO'));
  readonly heroTextoCta = computed(() => this.tenant()?.heroTextoCta?.trim() || 'Agenda');
  readonly heroAccionCta = computed(() => this.siteAction(this.tenant()?.heroAccionCta));
  readonly heroEnlaceCta = computed(() => this.safeExternalUrl(this.tenant()?.heroEnlaceCta));
  readonly heroTextoSecundario = computed(() => this.tenant()?.heroTextoSecundario?.trim() || 'WhatsApp');
  readonly heroAccionSecundaria = computed(() => this.siteAction(this.tenant()?.heroAccionSecundaria, 'WHATSAPP'));
  readonly heroEnlaceSecundario = computed(() => this.safeExternalUrl(this.tenant()?.heroEnlaceSecundario));
  readonly heroMostrarSecundario = computed(() => this.tenant()?.heroMostrarSecundario ?? this.tenant()?.heroMostrarWhatsapp ?? true);
  readonly heroMostrarWhatsapp = computed(() => this.tenant()?.heroMostrarWhatsapp ?? true);
  readonly inicioTituloServicios = computed(() => this.tenant()?.inicioTituloServicios ?? '');
  readonly inicioDescripcionServicios = computed(() => this.tenant()?.inicioDescripcionServicios ?? '');
  readonly inicioEtiquetaServicios = computed(() => this.tenant()?.inicioEtiquetaServicios?.trim() || 'Servicios');
  readonly inicioTextoVerServicios = computed(() => this.tenant()?.inicioTextoVerServicios?.trim() || 'Ver servicios');
  readonly inicioTextoReservar = computed(() => this.tenant()?.inicioTextoReservar?.trim() || 'Reservar');
  readonly inicioTituloContacto = computed(() => this.tenant()?.inicioTituloContacto ?? '');
  readonly inicioDescripcionContacto = computed(() => this.tenant()?.inicioDescripcionContacto ?? '');
  readonly inicioEtiquetaContacto = computed(() => this.tenant()?.inicioEtiquetaContacto?.trim() || 'Contacto');
  readonly inicioTextoAgenda = computed(() => this.tenant()?.inicioTextoAgenda?.trim() || 'Ir a agenda');
  readonly inicioTextoWhatsapp = computed(() => this.tenant()?.inicioTextoWhatsapp?.trim() || 'Abrir WhatsApp');
  readonly inicioMostrarContacto = computed(() => this.tenant()?.inicioMostrarContacto ?? true);
  readonly inicioMostrarServicios = computed(() => this.tenant()?.inicioMostrarServicios ?? true);
  readonly inicioOrdenSecciones = computed(() => this.option(
    this.tenant()?.inicioOrdenSecciones,
    ['CONTACTO_SERVICIOS', 'SERVICIOS_CONTACTO'],
    'CONTACTO_SERVICIOS'
  ));
  readonly bloquesInicio = computed(() => {
    const configurados = this.tenant()?.bloques;
    if (configurados?.length) {
      return [...configurados].sort((a, b) => a.orden - b.orden);
    }

    const serviciosPrimero = this.inicioOrdenSecciones() === 'SERVICIOS_CONTACTO';
    return [
      { tipo: 'PORTADA', habilitado: true, orden: 0 },
      {
        tipo: serviciosPrimero ? 'SERVICIOS_DESTACADOS' : 'CONTACTO',
        habilitado: serviciosPrimero ? this.inicioMostrarServicios() : this.inicioMostrarContacto(),
        orden: 1
      },
      {
        tipo: serviciosPrimero ? 'CONTACTO' : 'SERVICIOS_DESTACADOS',
        habilitado: serviciosPrimero ? this.inicioMostrarContacto() : this.inicioMostrarServicios(),
        orden: 2
      }
    ].map(bloque => ({
      ...bloque,
      superficie: 'BASE' as const,
      presentacion: 'ABIERTA' as const,
      anchoContenido: 'NORMAL' as const,
      alineacion: 'IZQUIERDA' as const,
      separador: 'NINGUNO' as const,
      espaciado: 'NORMAL' as const,
      etiqueta: null,
      titulo: null,
      descripcion: null
    }));
  });
  readonly serviciosDestacados = computed(() => this.tenant()?.serviciosDestacados ?? null);
  readonly galeria = computed(() => this.tenant()?.galeria ?? null);
  readonly promociones = computed(() => this.tenant()?.promociones ?? []);
  readonly logoUrl = computed(() => this.tenant()?.logoUrl ?? null);
  readonly whatsapp = computed(() => this.tenant()?.whatsapp ?? null);
  readonly whatsappHabilitado = computed(() => (this.tenant()?.whatsappHabilitado ?? true) && Boolean(this.tenant()?.whatsapp));
  readonly whatsappMensaje = computed(() => this.tenant()?.whatsappMensaje?.trim() || 'Hola, quiero agendar una cita');
  readonly chatHabilitado = computed(() => this.tenant()?.chatHabilitado ?? true);
  readonly chatTitulo = computed(() => this.tenant()?.chatTitulo?.trim() || '¿Dudas? Chat con nosotros');
  readonly chatSaludo = computed(() => this.tenant()?.chatSaludo?.trim() || 'Hola, ¿en qué podemos ayudarte?');
  readonly telefono = computed(() => this.tenant()?.telefono ?? this.tenant()?.whatsapp ?? '');
  readonly correo = computed(() => this.tenant()?.correo ?? '');
  readonly direccion = computed(() => this.tenant()?.direccion ?? '');
  readonly instagramUrl = computed(() => this.tenant()?.instagramUrl ?? '');
  readonly facebookUrl = computed(() => this.tenant()?.facebookUrl ?? '');
  readonly xUrl = computed(() => this.tenant()?.xUrl ?? '');
  readonly tiktokUrl = computed(() => this.tenant()?.tiktokUrl ?? '');
  readonly tema = computed(() => this.option(this.tenant()?.tema, ['BASE', 'MINIMAL', 'ELEGANTE', 'VIBRANTE'], 'BASE'));
  readonly menuEstilo = computed(() => this.option(this.tenant()?.menuEstilo, ['FLOTANTE', 'SOLIDO'], 'FLOTANTE'));
  readonly menuMostrarInicio = computed(() => this.tenant()?.menuMostrarInicio ?? true);
  readonly menuMostrarServicios = computed(() => this.tenant()?.menuMostrarServicios ?? true);
  readonly menuMostrarContacto = computed(() => this.tenant()?.menuMostrarContacto ?? true);
  readonly menuMostrarRegistro = computed(() => this.tenant()?.menuMostrarRegistro ?? true);
  readonly menuMostrarDescripcion = computed(() => this.tenant()?.menuMostrarDescripcion ?? true);
  readonly menuTextoCta = computed(() => this.tenant()?.menuTextoCta?.trim() || 'Reservar');
  readonly menuAccionCta = computed(() => this.siteAction(this.tenant()?.menuAccionCta));
  readonly footerTexto = computed(() => this.tenant()?.footerTexto?.trim() || this.descripcionCorta());
  readonly footerMostrarNavegacion = computed(() => this.tenant()?.footerMostrarNavegacion ?? true);
  readonly footerMostrarContacto = computed(() => this.tenant()?.footerMostrarContacto ?? true);
  readonly beneficios = computed(() => this.tenant()?.beneficios ?? []);
  readonly equipo = computed(() => this.tenant()?.equipo ?? []);
  readonly resenas = computed(() => this.tenant()?.resenas ?? null);
  readonly preguntasFrecuentes = computed(() => this.tenant()?.preguntasFrecuentes ?? []);
  readonly socialLinks = computed(() => {
    const tenant = this.tenant();
    if (!tenant) return [];
    const facebookUrl = this.safeSocialUrl(tenant.facebookUrl);
    const instagramUrl = this.safeSocialUrl(tenant.instagramUrl);
    const xUrl = this.safeSocialUrl(tenant.xUrl);
    const tiktokUrl = this.safeSocialUrl(tenant.tiktokUrl);
    return [
      { id: 'whatsapp', label: 'WhatsApp', icon: 'bi-whatsapp', href: this.whatsappHref(), enabled: (tenant.whatsappHabilitado ?? true) && Boolean(tenant.whatsapp) },
      { id: 'facebook', label: 'Facebook', icon: 'bi-facebook', href: facebookUrl, enabled: (tenant.facebookHabilitado ?? true) && Boolean(facebookUrl) },
      { id: 'instagram', label: 'Instagram', icon: 'bi-instagram', href: instagramUrl, enabled: (tenant.instagramHabilitado ?? true) && Boolean(instagramUrl) },
      { id: 'x', label: 'X', icon: 'bi-twitter-x', href: xUrl, enabled: (tenant.xHabilitado ?? true) && Boolean(xUrl) },
      { id: 'tiktok', label: 'TikTok', icon: 'bi-tiktok', href: tiktokUrl, enabled: (tenant.tiktokHabilitado ?? true) && Boolean(tiktokUrl) }
    ];
  });

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

  bloqueInicioHabilitado(tipo: string): boolean {
    return this.bloquesInicio().find(bloque => bloque.tipo === tipo)?.habilitado ?? false;
  }

  ordenBloqueInicio(tipo: string): number {
    return this.bloquesInicio().find(bloque => bloque.tipo === tipo)?.orden ?? 999;
  }

  bloqueInicio(tipo: string): TenantSiteBlock | null {
    return this.bloquesInicio().find(bloque => bloque.tipo === tipo) ?? null;
  }

  clasesBloqueInicio(tipo: string): Record<string, boolean> {
    const bloque = this.bloqueInicio(tipo);
    if (!bloque) return {};
    return {
      [`home-section--surface-${bloque.superficie.toLowerCase()}`]: true,
      [`home-section--presentation-${bloque.presentacion.toLowerCase()}`]: true,
      [`home-section--width-${bloque.anchoContenido.toLowerCase()}`]: true,
      [`home-section--align-${bloque.alineacion.toLowerCase()}`]: true,
      [`home-section--separator-${bloque.separador.toLowerCase()}`]: true,
      [`home-section--space-${bloque.espaciado.toLowerCase()}`]: true
    };
  }

  actionRoute(action?: string | null): string {
    switch (this.siteAction(action)) {
      case 'SERVICIOS': return this.routeFor('servicios');
      case 'CONTACTO': return this.routeFor('contacto');
      case 'AGENDAR':
      default: return this.routeFor('agendar');
    }
  }

  actionHref(action?: string | null, externalUrl?: string | null): string {
    const normalized = this.siteAction(action);
    if (normalized === 'WHATSAPP') return this.whatsappHref();
    if (normalized === 'EXTERNO') return this.safeExternalUrl(externalUrl) || '#';
    return this.actionRoute(normalized);
  }

  actionIsExternal(action?: string | null): boolean {
    return ['WHATSAPP', 'EXTERNO'].includes(this.siteAction(action));
  }

  actionAvailable(action?: string | null, externalUrl?: string | null): boolean {
    const normalized = this.siteAction(action);
    if (normalized === 'WHATSAPP') return this.whatsappHabilitado();
    if (normalized === 'EXTERNO') return Boolean(this.safeExternalUrl(externalUrl));
    return true;
  }

  actionIcon(action?: string | null): string {
    switch (this.siteAction(action)) {
      case 'WHATSAPP': return 'bi bi-whatsapp';
      case 'SERVICIOS': return 'bi bi-grid';
      case 'CONTACTO': return 'bi bi-chat-dots';
      case 'EXTERNO': return 'bi bi-box-arrow-up-right';
      default: return 'bi bi-calendar2-check';
    }
  }

  whatsappHref(message?: string): string {
    const whatsapp = this.whatsapp();
    if (!whatsapp) {
      return '#';
    }

    return `https://wa.me/${whatsapp}?text=${encodeURIComponent(message?.trim() || this.whatsappMensaje())}`;
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

  private safeExternalUrl(value?: string | null): string {
    if (!value) return '';
    try {
      const url = new URL(value);
      return url.protocol === 'https:' || url.protocol === 'http:' ? url.toString() : '';
    } catch {
      return '';
    }
  }

  private safeSocialUrl(value?: string | null): string {
    return this.safeExternalUrl(value);
  }

  private siteAction(value?: string | null, fallback = 'AGENDAR'): string {
    return this.option(value, ['AGENDAR', 'SERVICIOS', 'CONTACTO', 'WHATSAPP', 'EXTERNO'], fallback);
  }

  private option(value: string | null | undefined, allowed: string[], fallback: string): string {
    const normalized = value?.trim().toUpperCase();
    return normalized && allowed.includes(normalized) ? normalized : fallback;
  }
}
