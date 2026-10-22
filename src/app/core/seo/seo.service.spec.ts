import { TestBed } from '@angular/core/testing';
import { SeoService } from './seo.service';
import { TenantSiteConfig } from '../tenant/tenant.types';
import { PlatformHostService } from '../platform/platform-host.service';

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
    TestBed.configureTestingModule({
      providers: [{
        provide: PlatformHostService,
        useValue: {
          marketingUrl: (path = '') => `https://example.test${path.startsWith('/') ? path : `/${path}`}`
        }
      }]
    });
    document.getElementById('tenant-structured-data')?.remove();
  });

  it('configura metadatos y canonical propios del tenant', () => {
    const service = TestBed.inject(SeoService);
    service.applyTenant(tenant, '/e/nail-art');

    expect(document.title).toBe('Nail Air | Reserva tu cita en línea');
    expect(document.querySelector<HTMLLinkElement>('link[rel="canonical"]')?.href)
      .toBe('https://example.test/e/nail-art');
    expect(document.querySelector<HTMLMetaElement>('meta[property="og:url"]')?.content)
      .toBe('https://example.test/e/nail-art');
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
    expect(schema['url']).toBe('https://example.test/e/nail-art');
  });

  it('marca como no indexable un sitio no publicado', () => {
    const service = TestBed.inject(SeoService);
    service.applyTenant({ ...tenant, publicado: false }, '/e/borrador');

    expect(document.querySelector<HTMLMetaElement>('meta[name="robots"]')?.content)
      .toBe('noindex, nofollow');
  });

  it('usa el titulo y la descripcion SEO configurados por el tenant', () => {
    const service = TestBed.inject(SeoService);
    service.applyTenant({
      ...tenant,
      seoTitulo: 'Manicure profesional en Puebla',
      seoDescripcion: 'Reserva manicure, gel y diseño de uñas en línea.'
    }, '/e/nail-art');

    expect(document.title).toBe('Manicure profesional en Puebla');
    expect(document.querySelector<HTMLMetaElement>('meta[name="description"]')?.content)
      .toBe('Reserva manicure, gel y diseño de uñas en línea.');
  });

  it('limpia la identidad del tenant al abrir una landing comercial', () => {
    const service = TestBed.inject(SeoService);
    service.applyTenant(tenant);
    service.applyLanding({slug:'barberias',seoTitulo:'Barberías | Fluora',seoDescripcion:'Organiza citas',imagenUrl:null});
    expect(document.title).toBe('Barberías | Fluora');
    expect(document.querySelector<HTMLLinkElement>('link[rel="canonical"]')?.href).toBe('https://example.test/barberias');
    expect(document.querySelector('meta[property="og:image"]')).toBeNull();
    const schema = JSON.parse(document.getElementById('tenant-structured-data')?.textContent || '{}');
    expect(schema['@type']).toBe('WebPage');
    expect(schema['telephone']).toBeUndefined();
  });
  it('conserva el canonical que devuelve backend en dominio y slug, sin inferir planes', () => {
    const service=TestBed.inject(SeoService);
    const config={...tenant, dominioPrincipal:'reservas.fixture.mx'};
    for (const path of ['/servicios','/e/nail-art/servicios?utm_source=fixture']) {
      service.applyTenant(config,path);
      expect(document.querySelector<HTMLLinkElement>('link[rel="canonical"]')?.href).toBe('https://reservas.fixture.mx/servicios');
    }
    service.applyTenant({...config,dominioPrincipal:null},'/e/nail-art');
    expect(document.querySelector<HTMLLinkElement>('link[rel="canonical"]')?.href).toBe('https://example.test/e/nail-art');
  });
  it('publica enlaces de productos con el mismo canonical del backend', () => {
    const service=TestBed.inject(SeoService);
    service.applyProductCatalog({...tenant,dominioPrincipal:'fixture.mx'},{tienda:{},productos:[{nombre:'Artículo',slug:'articulo',descripcion:null,imagenUrl:null,precio:80,moneda:'MXN',disponible:true}]});
    expect(document.querySelector<HTMLLinkElement>('link[rel="canonical"]')?.href).toBe('https://fixture.mx/productos');
    expect(JSON.parse(document.getElementById('tenant-structured-data')?.textContent||'{}').itemListElement[0].url).toBe('https://fixture.mx/productos/articulo');
  });
});
