import { MarketingTrackingService, readAttribution } from './marketing-tracking.service';

describe('MarketingTrackingService', () => {
  const runtime = globalThis as typeof globalThis & { __FLUORA_MARKETING_TRACKING__?: {enabled?: boolean}; dataLayer?: Record<string, unknown>[] };
  beforeEach(() => { sessionStorage.removeItem('fluora_marketing_attribution'); delete runtime.__FLUORA_MARKETING_TRACKING__; delete runtime.dataLayer; });
  afterEach(() => { sessionStorage.removeItem('fluora_marketing_attribution'); delete runtime.__FLUORA_MARKETING_TRACKING__; delete runtime.dataLayer; });

  it('rechaza correo, teléfonos y parámetros ajenos al contrato', () => {
    const value = readAttribution(new URLSearchParams('utm_source=google&utm_campaign=persona%40example.com&utm_term=telefono5512345678&token=secreto&correo=persona%40example.com'), 'unas');
    expect(value).toEqual({vertical:'unas',campaign:{utm_source:'google'}});
  });

  it('conserva la atribución entre dominios mediante el enlace y mantiene el plan real', () => {
    const service = new MarketingTrackingService();
    service.capture('?utm_source=google&utm_medium=cpc&utm_campaign=unas_octubre&token=secreto', 'unas');
    const url = new URL(service.registrationUrl('https://app.example.test/registro', 7));
    expect(url.hostname).toBe('app.example.test');
    expect(url.searchParams.get('plan')).toBe('7');
    expect(url.searchParams.get('utm_campaign')).toBe('unas_octubre');
    expect(url.searchParams.get('landing_vertical')).toBe('unas');
    expect(url.searchParams.has('token')).toBe(false);
    expect(new MarketingTrackingService().registrationUrl('https://app.example.test/registro')).toContain('utm_campaign=unas_octubre');
  });

  it('no emite sin consentimiento y configuración explícita; limita los campos enviados', () => {
    const service = new MarketingTrackingService();
    service.capture('?utm_source=google&utm_content=anuncio_a&utm_term=manicure', 'unas');
    runtime.__FLUORA_MARKETING_TRACKING__={enabled:true};
    service.emit('landing_view');
    expect(runtime.dataLayer).toBeUndefined();
    service.setConsent(true);
    service.emit('signup_completed');
    expect(runtime.dataLayer).toEqual([{event:'signup_completed',vertical:'unas',source:'google'}]);
  });

  it('vuelve a validar atribución manipulada en almacenamiento', () => {
    sessionStorage.setItem('fluora_marketing_attribution',JSON.stringify({vertical:'../privado',campaign:{utm_source:'persona@example.com'},token:'secreto'}));
    expect(new MarketingTrackingService().registrationUrl('https://app.example.test/registro')).toBe('https://app.example.test/registro');
  });
});
