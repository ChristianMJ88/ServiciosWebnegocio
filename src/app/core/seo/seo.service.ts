import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { TenantSiteConfig } from '../tenant/tenant.types';
import { PlatformHostService } from '../platform/platform-host.service';

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly document = inject(DOCUMENT);
  private readonly meta = inject(Meta);
  private readonly title = inject(Title);
  private readonly platformHost = inject(PlatformHostService);

  applyTenant(config: TenantSiteConfig, path = `/e/${config.slug}`): void {
    const canonicalPath = path.split('?')[0].split('#')[0] || `/e/${config.slug}`;
    const canonical = this.tenantUrl(config, canonicalPath);
    const description = config.seoDescripcion?.trim() || this.tenantDescription(config);
    const pageTitle = config.seoTitulo?.trim() || `${config.nombreComercial} | Reserva tu cita en línea`;
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

  applyLanding(landing: { slug: string; seoTitulo: string; seoDescripcion: string; imagenUrl: string | null }): void {
    const canonical = this.platformHost.marketingUrl('/' + landing.slug);
    this.title.setTitle(landing.seoTitulo);
    this.meta.updateTag({ name: 'description', content: landing.seoDescripcion });
    this.meta.updateTag({ name: 'robots', content: 'index, follow, max-image-preview:large' });
    this.meta.removeTag("name='keywords'");
    for (const [key, value] of Object.entries({
      'og:title': landing.seoTitulo, 'og:description': landing.seoDescripcion,
      'og:type': 'website', 'og:url': canonical, 'og:locale': 'es_MX'
    })) this.meta.updateTag({ property: key, content: value });
    this.meta.updateTag({name:'twitter:card', content:landing.imagenUrl ? 'summary_large_image' : 'summary'});
    this.meta.updateTag({name:'twitter:title', content:landing.seoTitulo});
    this.meta.updateTag({name:'twitter:description', content:landing.seoDescripcion});
    if (landing.imagenUrl) {
      this.meta.updateTag({property:'og:image', content:landing.imagenUrl});
      this.meta.updateTag({name:'twitter:image', content:landing.imagenUrl});
    } else {
      this.meta.removeTag("property='og:image'");
      this.meta.removeTag("name='twitter:image'");
    }
    this.setCanonical(canonical);
    this.setStructuredData({'@context':'https://schema.org', '@type':'WebPage',
      name:landing.seoTitulo, description:landing.seoDescripcion, url:canonical});
  }

  applyUnavailableMarketing(): void {
    this.applyMarketing();
    this.title.setTitle('Página no disponible | Fluora');
    this.meta.updateTag({name:'robots', content:'noindex, nofollow'});
    this.meta.updateTag({name:'description', content:'Esta página no está disponible.'});
  }

  applyMarketing(): void {
    this.setCanonical(this.platformHost.marketingUrl('/'));
    this.removeTenantStructuredData();
    this.meta.updateTag({name:'robots',content:'index, follow'});
  }

  applyDirectory(): void {
    this.removeTenantStructuredData();
    this.setCanonical(this.platformHost.marketingUrl('/negocios'));
    this.title.setTitle('Directorio de negocios | Fluora');
    this.meta.updateTag({name:'description',content:'Encuentra negocios y servicios. Consulta sus datos públicos y agenda directamente con cada negocio.'});
    // Las variantes de búsqueda mantienen el canonical del directorio.
    this.meta.updateTag({name:'robots',content:this.document.location?.search ? 'noindex, follow' : 'index, follow'});
  }

  applyProductCatalog(config: TenantSiteConfig, catalog: {
    tienda: { seoTitulo?: string | null; seoDescripcion?: string | null; titulo?: string | null; descripcion?: string | null };
    productos: Array<{ nombre: string; slug: string; descripcion: string | null; imagenUrl: string | null; precio: number; moneda: string; disponible: boolean }>;
  }, product?: { nombre: string; slug: string; descripcion: string | null; imagenUrl: string | null; precio: number; moneda: string; disponible: boolean } | null): void {
    const path = product ? `/e/${config.slug}/productos/${product.slug}` : `/e/${config.slug}/productos`;
    const canonical = this.tenantUrl(config, path);
    const title = product
      ? `${product.nombre} | ${config.nombreComercial}`
      : catalog.tienda.seoTitulo?.trim() || `${catalog.tienda.titulo?.trim() || 'Productos'} | ${config.nombreComercial}`;
    const description = product?.descripcion?.trim()
      || catalog.tienda.seoDescripcion?.trim()
      || catalog.tienda.descripcion?.trim()
      || `Compra productos de ${config.nombreComercial}.`;
    const image = product?.imagenUrl || config.logoUrl || config.heroImagenUrl;

    this.title.setTitle(title);
    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ name: 'robots', content: config.publicado ? 'index, follow, max-image-preview:large' : 'noindex, nofollow' });
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:type', content: product ? 'product' : 'website' });
    this.meta.updateTag({ property: 'og:url', content: canonical });
    this.meta.updateTag({ property: 'og:locale', content: 'es_MX' });
    this.meta.updateTag({ name: 'twitter:card', content: image ? 'summary_large_image' : 'summary' });
    this.meta.updateTag({ name: 'twitter:title', content: title });
    this.meta.updateTag({ name: 'twitter:description', content: description });
    if (image) {
      const absoluteImage = this.absoluteUrl(image);
      this.meta.updateTag({ property: 'og:image', content: absoluteImage });
      this.meta.updateTag({ name: 'twitter:image', content: absoluteImage });
    }
    this.setCanonical(canonical);
    this.setStructuredData(product
      ? this.productSchema(config, product, canonical, description)
      : this.productListSchema(config, catalog.productos, canonical));
  }

  private tenantUrl(config: TenantSiteConfig, path: string): string {
    const host = config.dominioPrincipal;
    if (host && /^(?=.{1,253}$)[a-z0-9-]+(?:\.[a-z0-9-]+)+$/.test(host)) {
      const prefix = `/e/${encodeURIComponent(config.slug)}`;
      const local = path === prefix || path === `${prefix}/` ? '/' : path.startsWith(`${prefix}/`) ? path.slice(prefix.length) : path;
      return `https://${host}${local}`;
    }
    return this.platformHost.marketingUrl(path);
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

    const sameAs = [
      config.instagramHabilitado !== false ? config.instagramUrl : null,
      config.facebookHabilitado !== false ? config.facebookUrl : null,
      config.xHabilitado !== false ? config.xUrl : null,
      config.tiktokHabilitado !== false ? config.tiktokUrl : null
    ].filter((value): value is string => Boolean(value));
    if (sameAs.length) schema['sameAs'] = sameAs;
    return schema;
  }

  private productListSchema(config: TenantSiteConfig, products: Array<{ nombre: string; slug: string }>, url: string): Record<string, unknown> {
    return {
      '@context': 'https://schema.org', '@type': 'ItemList', name: `Productos de ${config.nombreComercial}`,
      url, numberOfItems: products.length,
      itemListElement: products.map((product, index) => ({
        '@type': 'ListItem', position: index + 1, name: product.nombre,
        url: this.tenantUrl(config, `/e/${config.slug}/productos/${product.slug}`)
      }))
    };
  }

  private productSchema(config: TenantSiteConfig,
                        product: { nombre: string; imagenUrl: string | null; precio: number; moneda: string; disponible: boolean },
                        url: string, description: string): Record<string, unknown> {
    const schema: Record<string, unknown> = {
      '@context': 'https://schema.org', '@type': 'Product', name: product.nombre, description, url,
      brand: { '@type': 'Brand', name: config.nombreComercial },
      offers: {
        '@type': 'Offer', url, priceCurrency: product.moneda, price: product.precio.toFixed(2),
        availability: product.disponible ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock'
      }
    };
    if (product.imagenUrl) schema['image'] = this.absoluteUrl(product.imagenUrl);
    return schema;
  }

  private absoluteUrl(value: string): string {
    if (/^https?:\/\//i.test(value)) return value;
    return this.platformHost.marketingUrl(value);
  }
}
