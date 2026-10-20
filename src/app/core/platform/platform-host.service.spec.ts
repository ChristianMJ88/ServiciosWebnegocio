import { TestBed } from '@angular/core/testing';
import { afterEach, vi } from 'vitest';
import { isMarketingHostname, PlatformHostService } from './platform-host.service';

describe('PlatformHostService: caracterización del navegador antes de R4', () => {
  afterEach(() => vi.unstubAllGlobals());
  it.each([
    ['https://marketing.test', 'marketing.test', true, false, false],
    ['https://www.marketing.test', 'www.marketing.test', true, false, false],
    ['https://app.test', 'app.test', false, true, false],
    ['http://localhost:4200', 'localhost', false, false, false],
    ['https://reservas.negocio.test', 'reservas.negocio.test', false, false, true]
  ])('conserva clasificación y enlaces de %s', (origin, hostname, marketing, app, tenant) => {
    vi.stubGlobal('location', {origin,hostname});
    vi.stubGlobal('__FLUORA_MARKETING_ORIGIN__', 'https://marketing.test');
    vi.stubGlobal('__FLUORA_APP_ORIGIN__', 'https://app.test');
    const host = TestBed.inject(PlatformHostService);
    expect(host.currentOrigin()).toBe(origin);
    expect(host.currentHostname()).toBe(hostname);
    expect(host.isMarketingHost()).toBe(marketing);
    expect(host.isAppHost()).toBe(app);
    expect(host.isCustomTenantHost()).toBe(tenant);
    expect(host.appUrl('/registro')).toBe('https://app.test/registro');
    expect(host.marketingUrl('/')).toBe('https://marketing.test/');
  });
  it('conserva el origen integrado cuando no hay configuración de orígenes', () => {
    vi.stubGlobal('location', {origin:'http://localhost:4200',hostname:'localhost'});
    vi.stubGlobal('__FLUORA_MARKETING_ORIGIN__', undefined);
    vi.stubGlobal('__FLUORA_APP_ORIGIN__', undefined);
    const host = TestBed.inject(PlatformHostService);
    expect(host.hasDedicatedAppHost()).toBe(false);
    expect(host.isAppHost()).toBe(false);
    expect(host.isCustomTenantHost()).toBe(false);
    expect(host.appUrl('/acceso')).toBe('http://localhost:4200/acceso');
  });
  it('normaliza espacios y barras finales en los orígenes configurados del navegador', () => {
    vi.stubGlobal('location',{origin:'https://marketing.test',hostname:'marketing.test'});
    vi.stubGlobal('__FLUORA_MARKETING_ORIGIN__','  https://marketing.test/  ');
    vi.stubGlobal('__FLUORA_APP_ORIGIN__','  https://app.test/  ');
    const host=TestBed.inject(PlatformHostService);
    expect(host.isMarketingHost()).toBe(true);expect(host.appUrl('/registro')).toBe('https://app.test/registro');
    expect(host.marketingUrl('/')).toBe('https://marketing.test/');
  });
  it('conserva el host vacío y los enlaces relativos cuando location y runtime no existen', () => {
    vi.stubGlobal('location',undefined);vi.stubGlobal('__FLUORA_MARKETING_ORIGIN__',undefined);vi.stubGlobal('__FLUORA_APP_ORIGIN__',undefined);
    const host=TestBed.inject(PlatformHostService);
    expect(host.currentOrigin()).toBe('');expect(host.currentHostname()).toBe('');expect(host.appUrl('/registro')).toBe('/registro');
    expect(host.isMarketingHost()).toBe(false);expect(host.isCustomTenantHost()).toBe(true);
  });
});

describe('PlatformHostService host classification', () => {
  it('recognizes the configured marketing hostname', () => {
    expect(isMarketingHostname('refluora.com', 'refluora.com')).toBe(true);
  });

  it('recognizes the www alias of the configured marketing hostname', () => {
    expect(isMarketingHostname('www.refluora.com', 'refluora.com')).toBe(true);
  });

  it('does not classify tenant or application hosts as marketing', () => {
    expect(isMarketingHostname('app.refluora.com', 'refluora.com')).toBe(false);
    expect(isMarketingHostname('reservas.negocio.mx', 'refluora.com')).toBe(false);
  });
});
