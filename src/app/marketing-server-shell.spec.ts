import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { DeferBlockBehavior, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router } from '@angular/router';
import { afterEach, vi } from 'vitest';
import { App } from './app';
import { marketingRoutes } from './app.routes.marketing';
import { provideMarketingRenderContext } from './core/platform/marketing-render-context';
import { provideTenantPublicStyles } from './core/styles/tenant-public-styles';
import { TenantSiteService } from './core/tenant/tenant-site.service';
import { FluoraHomeComponent } from './pages/fluora-home/fluora-home.component';
import { MarketingNavComponent } from './components/marketing-nav/marketing-nav.component';
import { AuthService } from './core/auth/auth.service';

describe('árbol de marketing: contrato de servidor simulado (jsdom)', () => {
  afterEach(()=>{vi.restoreAllMocks();vi.unstubAllGlobals();});
  it('renderiza Home y navegación de plataforma sin datos, sesión ni efectos de navegador', async () => {
    TestBed.configureTestingModule({imports:[App],deferBlockBehavior:DeferBlockBehavior.Manual,
      providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter(marketingRoutes),provideTenantPublicStyles(),
        {provide:PLATFORM_ID,useValue:'server'},
        provideMarketingRenderContext({route:'/',marketingOrigin:'https://marketing.test',appOrigin:'https://app.test'})]});
    await TestBed.compileComponents();
    const forbidden=vi.fn(()=>{throw Error('Browser effect in simulated server');});
    const storage=['getItem','setItem','removeItem'].map(method=>vi.spyOn(Storage.prototype,method as 'getItem').mockImplementation(forbidden));
    const observed:Element[]=[];
    // jsdom still runs Angular's browser @defer machinery; never notify its placeholder observers.
    // The separate directive test poisons IO and proves the app's scrollspy skips it on server.
    vi.stubGlobal('IntersectionObserver',class {observe(element:Element){observed.push(element);}unobserve(){}disconnect(){}});
    const media=vi.fn(forbidden),resize=vi.fn(forbidden);
    vi.stubGlobal('matchMedia',media);vi.stubGlobal('ResizeObserver',resize);
    const consoleError=vi.spyOn(console,'error');
    // Angular's test renderer itself may schedule a frame. Check app effects via IO/media/listeners,
    // and the dedicated scrollspy browser regression tests cover RAF registration/cleanup.
    const windowListeners=vi.spyOn(window,'addEventListener'),documentListeners=vi.spyOn(document,'addEventListener');
    const animation=vi.spyOn(FluoraHomeComponent.prototype as unknown as {loadAnimation:()=>Promise<unknown>},'loadAnimation');
    const tenant=TestBed.inject(TenantSiteService),byHost=vi.spyOn(tenant,'getByHostname'),bySlug=vi.spyOn(tenant,'getBySlug');
    const fixture=TestBed.createComponent(App);fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/');fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-marketing-nav')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-fluora-home')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-header')).toBeNull();
    expect(fixture.componentInstance.esNavegacionMarketing()).toBe(true);
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
    expect(fixture.nativeElement.querySelectorAll('.section-placeholder').length).toBe(4);
    expect(fixture.nativeElement.querySelector('app-home-plans,app-home-faq,app-home-product,app-home-closing')).toBeNull();
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('La agenda en línea');
    expect(fixture.nativeElement.querySelector('.top-cta').href).toBe('https://app.test/registro');
    const home=fixture.debugElement.query(By.directive(FluoraHomeComponent)).componentInstance as FluoraHomeComponent;
    expect(home.offer.plans()).toEqual([]);expect(home.offer.loading()).toBe(true);expect(home.support()).toBeNull();expect(home.verticals()).toEqual([]);
    expect(byHost).not.toHaveBeenCalled();expect(bySlug).not.toHaveBeenCalled();expect(animation).not.toHaveBeenCalled();
    for(const spy of storage)expect(spy).not.toHaveBeenCalled();
    for(const [type] of windowListeners.mock.calls)expect(['scroll','wheel','touchstart','keydown','resize']).not.toContain(type);
    for(const [type] of documentListeners.mock.calls)expect(['click','keydown']).not.toContain(type);
    expect(observed.some(element=>['inicio','como-funciona','producto','planes','faq'].includes(element.id))).toBe(false);
    expect(consoleError).not.toHaveBeenCalled();
    expect(media).not.toHaveBeenCalled();expect(resize).not.toHaveBeenCalled();
    expect(document.documentElement.style.scrollbarGutter).toBe('');
    expect(document.documentElement.dataset['tenantTheme']).toBeUndefined();
    expect(document.documentElement.style.getPropertyValue('--tenant-primary')).toBe('');
    expect(document.querySelector('link[data-tenant-public-styles]')).toBeNull();
    const http=TestBed.inject(HttpTestingController);expect(http.match(()=>true)).toEqual([]);http.verify();
    fixture.destroy();
  });
  it('control positivo: los mismos targets detectan sesión y listeners en navegador', async () => {
    TestBed.configureTestingModule({imports:[MarketingNavComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:PLATFORM_ID,useValue:'browser'}]});
    await TestBed.compileComponents();
    expect(document.defaultView).toBe(window);
    const storage=vi.spyOn(Storage.prototype,'getItem');
    const windowListeners=vi.spyOn(window,'addEventListener'),documentListeners=vi.spyOn(document,'addEventListener');
    const media=vi.fn(()=>({matches:false,addEventListener:vi.fn(),removeEventListener:vi.fn()}));
    vi.stubGlobal('matchMedia',media);
    TestBed.inject(AuthService);
    const fixture=TestBed.createComponent(MarketingNavComponent);fixture.detectChanges();
    expect(storage).toHaveBeenCalled();expect(media).toHaveBeenCalledWith('(min-width:1200px)');
    expect(windowListeners.mock.calls.some(([type])=>type==='scroll')).toBe(true);
    expect(documentListeners.mock.calls.some(([type])=>type==='click')).toBe(true);
    expect(documentListeners.mock.calls.some(([type])=>type==='keydown')).toBe(true);
    const http=TestBed.inject(HttpTestingController);for(const request of http.match(()=>true))request.flush([]);http.verify();
    fixture.destroy();
  });
});
