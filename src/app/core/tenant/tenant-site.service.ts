import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TenantSiteConfig } from './tenant.types';

const DEMO_TENANT: TenantSiteConfig = {
  empresaId: 1,
  slug: 'nail-air',
  nombreComercial: 'Nail Art Studio',
  dominioPrincipal: null,
  logoUrl: '/NailArt_logo.jpeg',
  descripcionCorta: 'Diseño, técnica y cuidado para manos y pies.',
  colorPrimario: '#d14f7d',
  colorSecundario: '#f6d9e3',
  fuenteTitulos: 'JAKARTA',
  fuenteCuerpo: 'INTER',
  heroTitulo: 'Diseños que se sienten tuyos desde el primer vistazo.',
  heroSubtitulo: 'Reserva, conoce servicios y atiende a tus clientes con una experiencia clara y cuidada.',
  heroImagenUrl: '/tenant-hero-demo.png',
  whatsapp: '522204292573',
  telefono: '220 429 25 73',
  correo: 'christianmejia@techprotech.com.mx',
  direccion: 'Calle Diagonal Benito Juarez #19, Col. Nueva Antequera, Puebla',
  instagramUrl: '',
  facebookUrl: '',
  tema: 'nail-art-base',
  publicado: true
};

@Injectable({
  providedIn: 'root'
})
export class TenantSiteService {
  private readonly http = inject(HttpClient);

  getBySlug(slug: string): Observable<TenantSiteConfig> {
    const slugNormalizado = slug.trim().toLowerCase();
    if (!slugNormalizado) {
      return of(DEMO_TENANT);
    }

    if (!environment.apiBaseUrl) {
      return of(this.resolveFallback(slugNormalizado));
    }

    return this.http
      .get<TenantSiteConfig>(`${environment.apiBaseUrl}/publico/sitio/${slugNormalizado}`)
      .pipe(catchError(error => slugNormalizado === DEMO_TENANT.slug
        ? of(DEMO_TENANT)
        : throwError(() => error)));
  }

  getByHostname(hostname: string): Observable<TenantSiteConfig> {
    const hostNormalizado = hostname.trim().toLowerCase();
    if (!hostNormalizado) {
      return of(DEMO_TENANT);
    }

    if (!environment.apiBaseUrl) {
      return of(DEMO_TENANT);
    }

    return this.http
      .get<TenantSiteConfig>(`${environment.apiBaseUrl}/publico/sitio/host/${encodeURIComponent(hostNormalizado)}`)
      .pipe(catchError(() => of(DEMO_TENANT)));
  }

  private resolveFallback(slug: string): TenantSiteConfig {
    if (slug === DEMO_TENANT.slug) {
      return DEMO_TENANT;
    }

    return {
      ...DEMO_TENANT,
      slug,
      nombreComercial: this.titleFromSlug(slug),
      dominioPrincipal: null,
      heroTitulo: `Bienvenido a ${this.titleFromSlug(slug)}.`,
      heroSubtitulo: 'Este tenant todavía no tiene una configuración pública completa.',
      heroImagenUrl: DEMO_TENANT.heroImagenUrl,
      publicado: false
    };
  }

  private titleFromSlug(slug: string): string {
    return slug
      .split('-')
      .filter(Boolean)
      .map(fragment => fragment.charAt(0).toUpperCase() + fragment.slice(1))
      .join(' ');
  }
}
