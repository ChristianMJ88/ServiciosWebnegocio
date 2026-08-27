import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { TenantSiteConfig } from '../tenant/tenant.types';

const MARKETING_ORIGIN = 'https://refluora.com';

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly document = inject(DOCUMENT);
  private readonly meta = inject(Meta);
  private readonly title = inject(Title);

  applyTenant(config: TenantSiteConfig, path = `/e/${config.slug}`): void {
    const canonicalPath = path.split('?')[0].split('#')[0] || `/e/${config.slug}`;
    const canonical = `${MARKETING_ORIGIN}${canonicalPath}`;
    const description = this.tenantDescription(config);
    const pageTitle = `${config.nombreComercial} | Reserva tu cita en línea`;
    const image = config.heroImagenUrl || config.logoUrl;

    this.title.setTitle(pageTitle);
    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ name: 'robots', content: config.publicado ? 'index, follow, max-image-preview:large' : 'noindex, nofollow' });
    this.meta.updateTag({ property: 'og:title', content: pageTitle });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:type', content: 'business.business' });
    this.meta.updateTag({ property: 'og:url', content: canonical });
    this.meta.updateTag({ property: 'og:locale', content: 'es_MX' });
    this.meta.updateTag({ name: 'twitter:card', content: image ? 'summary_large_image' : 'summary' });
    this.meta.updateTag({ name: 'twitter:title', content: pageTitle });
    this.meta.updateTag({ name: 'twitter:description', content: description });

    if (image) {
      const absoluteImage = this.absoluteUrl(image);
      this.meta.updateTag({ property: 'og:image', content: absoluteImage });
      this.meta.updateTag({ name: 'twitter:image', content: absoluteImage });
    } else {
      this.meta.removeTag("property='og:image'");
      this.meta.removeTag("name='twitter:image'");
    }

    this.setCanonical(canonical);
    this.setStructuredData(this.localBusinessSchema(config, canonical, description, image));
  }

  applyMarketing(): void {
    this.setCanonical(`${MARKETING_ORIGIN}/`);
    this.removeTenantStructuredData();
  }

  private tenantDescription(config: TenantSiteConfig): string {
    return config.descripcionCorta?.trim()
      || config.heroSubtitulo?.trim()
      || `Conoce los servicios de ${config.nombreComercial} y reserva tu cita en línea.`;
  }

  private setCanonical(url: string): void {
    let link = this.document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.rel = 'canonical';
      this.document.head.appendChild(link);
    }
    link.href = url;
  }

  private setStructuredData(data: Record<string, unknown>): void {
    this.removeTenantStructuredData();
    const script = this.document.createElement('script');
    script.id = 'tenant-structured-data';
    script.type = 'application/ld+json';
    script.textContent = JSON.stringify(data).replace(/</g, '\\u003c');
    this.document.head.appendChild(script);
  }

  private removeTenantStructuredData(): void {
    this.document.getElementById('tenant-structured-data')?.remove();
  }

  private localBusinessSchema(
    config: TenantSiteConfig,
    url: string,
    description: string,
    image: string | null
  ): Record<string, unknown> {
    const schema: Record<string, unknown> = {
      '@context': 'https://schema.org',
      '@type': 'LocalBusiness',
      '@id': `${url}#business`,
      name: config.nombreComercial,
      description,
      url
    };

    if (image) schema['image'] = this.absoluteUrl(image);
    if (config.logoUrl) schema['logo'] = this.absoluteUrl(config.logoUrl);
    if (config.telefono || config.whatsapp) schema['telephone'] = config.telefono || config.whatsapp;
    if (config.correo) schema['email'] = config.correo;
    if (config.direccion) {
      schema['address'] = { '@type': 'PostalAddress', streetAddress: config.direccion };
    }

    const sameAs = [config.instagramUrl, config.facebookUrl].filter((value): value is string => Boolean(value));
    if (sameAs.length) schema['sameAs'] = sameAs;
    return schema;
  }

  private absoluteUrl(value: string): string {
    if (/^https?:\/\//i.test(value)) return value;
    return `${MARKETING_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`;
  }
}
