import { DOCUMENT } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, vi } from 'vitest';
import type { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { PlatformAuthService } from '../platform/platform-auth.service';
import { PublicPlansService } from '../platform/public-plans.service';
import { provideTenantPublicStyles, TENANT_PUBLIC_STYLES } from '../styles/tenant-public-styles';
import { SubscriptionAccessNoticeComponent } from '../subscription/subscription-access-notice.component';
import { SubscriptionAccessService } from '../subscription/subscription-access.service';
import { LandingMarketingService } from './landing-marketing.service';
import { PlatformContactService } from './platform-contact.service';
import { MarketingTrackingService } from './marketing-tracking.service';
import { prepareMarketingHome } from './marketing-bootstrap';

describe('dependencias de marketing: contrato de servidor simulado', () => {
  afterEach(()=>{vi.restoreAllMocks();vi.useRealTimers();vi.unstubAllGlobals();});
  function setup(server=true) {
    TestBed.configureTestingModule({providers:[{provide:PLATFORM_ID,useValue:server?'server':'browser'},provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});
  }
  function poisonStorage() {
    return ['getItem','setItem','removeItem'].map(method=>vi.spyOn(Storage.prototype,method as 'getItem').mockImplementation(()=>{throw Error('Storage read/write in server contract');}));
  }
  it('no restaura ni persiste sesiones de negocio/plataforma en servidor', () => {
    setup();const storage=poisonStorage();
    const auth=TestBed.inject(AuthService),platform=TestBed.inject(PlatformAuthService);
    expect(auth.sesionActual()).toBeNull();expect(auth.getTokenAcceso()).toBeNull();
    expect(auth.sincronizarSesionPersistida()).toBe(false);
    expect(platform.sesionActual()).toBeNull();expect(platform.token()).toBeNull();
    auth.limpiarSesion();
    for(const spy of storage)expect(spy).not.toHaveBeenCalled();
    TestBed.inject(HttpTestingController).verify();
  });
  it.each([true,false])('el reloj de avisos solo existe en navegador (servidor=%s)', server => {
    vi.useFakeTimers();setup(server);
    const refresh=vi.spyOn(TestBed.inject(SubscriptionAccessService),'refresh');
    const fixture=TestBed.createComponent(SubscriptionAccessNoticeComponent);fixture.detectChanges();
    expect(vi.getTimerCount()).toBe(server?0:1);
    vi.advanceTimersByTime(120000);
    expect(refresh).not.toHaveBeenCalled();
    fixture.destroy();expect(vi.getTimerCount()).toBe(0);
  });
  it('los servicios públicos no solicitan datos ni emiten valores sintéticos en servidor', () => {
    setup();const next=vi.fn(),complete=vi.fn();
    const requests:Observable<unknown>[]=[TestBed.inject(PublicPlansService).listar(),TestBed.inject(LandingMarketingService).navegacion(),TestBed.inject(PlatformContactService).publicSupport()];
    for(const request of requests)request.subscribe({next,complete});
    expect(next).not.toHaveBeenCalled();expect(complete).toHaveBeenCalledTimes(3);
    TestBed.inject(HttpTestingController).verify();
  });
  it('no lee ni guarda atribución y no emite tracking aunque haya consentimiento en servidor', () => {
    setup();const storage=poisonStorage();vi.stubGlobal('__FLUORA_MARKETING_TRACKING__',{enabled:true});vi.stubGlobal('dataLayer',undefined);
    const tracking=TestBed.inject(MarketingTrackingService);
    tracking.capture('?utm_source=google','salon');tracking.setConsent(true);tracking.emit('landing_view');
    expect(tracking.registrationUrl('https://app.test/registro')).toBe('https://app.test/registro');
    expect((globalThis as typeof globalThis & {dataLayer?:unknown}).dataLayer).toBeUndefined();
    for(const spy of storage)expect(spy).not.toHaveBeenCalled();
    expect(()=>tracking.registrationUrl('/registro')).toThrow();
  });
  it('el warmup y proveedor CSS no crean links, importaciones ni timers en servidor', async () => {
    setup();TestBed.overrideProvider(DOCUMENT,{useValue:{get head(){throw Error('DOM access');}}});
    TestBed.configureTestingModule({providers:[provideTenantPublicStyles()]});
    const load=vi.fn(()=>Promise.resolve({}));
    TestBed.runInInjectionContext(()=>prepareMarketingHome(load));
    expect(load).not.toHaveBeenCalled();
    const timeout=vi.spyOn(globalThis,'setTimeout');
    const styles=TestBed.inject(TENANT_PUBLIC_STYLES);
    expect(styles.ready()).toBe(true);await styles.ensure();expect(timeout).not.toHaveBeenCalled();
  });
});
