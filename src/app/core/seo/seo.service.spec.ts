import { TestBed } from '@angular/core/testing';
import { SeoService } from './seo.service';
import { TenantSiteConfig } from '../tenant/tenant.types';

describe('SeoService', () => {
  const tenant: TenantSiteConfig = {
    empresaId: 7,
    slug: 'nail-art',
    nombreComercial: 'Nail Air',
    dominioPrincipal: null,
    logoUrl: '/logo.png',
    descripcionCorta: 'Estudio de belleza y cuidado personal.',
    colorPrimario: '#123456',
    colorSecundario: '#654321',
    fuenteTitulos: 'INTER',
    fuenteCuerpo: 'INTER',
    heroTitulo: 'Nail Air',
    heroSubtitulo: 'Reserva tu cita.',
    heroImagenUrl: '/hero.png',
    whatsapp: '521234567890',
    telefono: '123 456 7890',
    correo: 'hola@nailair.test',
    direccion: 'Puebla, México',
    instagramUrl: 'https://instagram.com/nailair',
    facebookUrl: null,
    tema: null,
    publicado: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    document.getElementById('tenant-structured-data')?.remove();
  });

  it('configura metadatos y canonical propios del tenant', () => {
    const service = TestBed.inject(SeoService);
    service.applyTenant(tenant, '/e/nail-art');

    expect(document.title).toBe('Nail Air | Reserva tu cita en línea');
    expect(document.querySelector<HTMLLinkElement>('link[rel="canonical"]')?.href)
      .toBe('https://refluora.com/e/nail-art');
    expect(document.querySelector<HTMLMetaElement>('meta[property="og:url"]')?.content)
      .toBe('https://refluora.com/e/nail-art');
    expect(document.querySelector<HTMLMetaElement>('meta[name="robots"]')?.content)
      .toContain('index');
  });

  it('publica LocalBusiness sólo con los datos disponibles', () => {
    const service = TestBed.inject(SeoService);
    service.applyTenant(tenant, '/e/nail-art');

    const script = document.getElementById('tenant-structured-data');
    const schema = JSON.parse(script?.textContent || '{}') as Record<string, unknown>;
    expect(schema['@type']).toBe('LocalBusiness');
    expect(schema['name']).toBe('Nail Air');
    expect(schema['url']).toBe('https://refluora.com/e/nail-art');
  });

  it('marca como no indexable un sitio no publicado', () => {
    const service = TestBed.inject(SeoService);
    service.applyTenant({ ...tenant, publicado: false }, '/e/borrador');

    expect(document.querySelector<HTMLMetaElement>('meta[name="robots"]')?.content)
      .toBe('noindex, nofollow');
  });
});
