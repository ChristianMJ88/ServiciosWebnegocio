import { DeferBlockBehavior, DeferBlockState, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { NavigationCancel, NavigationCancellationCode, NavigationEnd, NavigationError, provideRouter, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { HeaderComponent } from './components/header/header.component';
import { Location } from '@angular/common';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlatformHostService } from './core/platform/platform-host.service';
import { vi } from 'vitest';
import { App } from './app';
import { TenantContextService } from './core/tenant/tenant-context.service';
import { signal } from '@angular/core';
import { TENANT_PUBLIC_STYLES } from './core/styles/tenant-public-styles';

@Component({ standalone: true, template: '<p id="route-content">Contenido público</p>' })
class PublicRouteProbe {}

function loadTenant(slug = 'negocio-prueba') {
  const tenant = TestBed.inject(TenantContextService);
  tenant.setTenant({
    empresaId:42,slug,nombreComercial:'Negocio de prueba',publicado:true,
    logoUrl:null,descripcionCorta:null,colorPrimario:null,colorSecundario:null,fuenteTitulos:null,fuenteCuerpo:null,
    heroTitulo:null,heroSubtitulo:null,heroImagenUrl:null,whatsapp:'+525555555555',telefono:null,correo:null,direccion:null,
    instagramUrl:null,facebookUrl:null,tema:null,whatsappHabilitado:true,chatHabilitado:true,
  });
  return tenant;
}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App, PublicRouteProbe],
      deferBlockBehavior: DeferBlockBehavior.Manual,
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
  it.each(['slug', 'host propio'])('conserva el tenant precargado antes de la primera navegación en %s', context => {
    vi.spyOn(TestBed.inject(Location), 'path').mockReturnValue(context === 'slug' ? '/e/negocio-prueba/servicios' : '/servicios');
    if (context === 'host propio') vi.spyOn(TestBed.inject(PlatformHostService), 'isCustomTenantHost').mockReturnValue(true);
    const tenant = loadTenant(), clear = vi.spyOn(tenant, 'clearTenant');
    expect(TestBed.inject(Router).url).toBe('/');
    TestBed.createComponent(App);
    expect(tenant.slug()).toBe('negocio-prueba');
    expect(clear).not.toHaveBeenCalled();
  });
  it.each(['slug','host propio'])('waits for tenant CSS before mounting Header on %s',async context=>{
    const ready=signal(false),ensure=vi.fn(()=>Promise.resolve());
    TestBed.overrideProvider(TENANT_PUBLIC_STYLES,{useValue:{ready,ensure}});
    if(context==='slug')vi.spyOn(TestBed.inject(Location),'path').mockReturnValue('/e/x');
    else vi.spyOn(TestBed.inject(PlatformHostService),'isCustomTenantHost').mockReturnValue(true);
    const fixture=TestBed.createComponent(App);fixture.detectChanges();
    expect(ensure).toHaveBeenCalledOnce();
    TestBed.inject(TenantContextService).setTenant({
      empresaId:42,slug:'negocio-prueba',nombreComercial:'Negocio de prueba',publicado:true,
      logoUrl:null,descripcionCorta:null,colorPrimario:null,colorSecundario:null,fuenteTitulos:null,fuenteCuerpo:null,
      heroTitulo:null,heroSubtitulo:null,heroImagenUrl:null,whatsapp:'+525555555555',telefono:null,correo:null,direccion:null,
      instagramUrl:null,facebookUrl:null,tema:null,whatsappHabilitado:true,chatHabilitado:true,
    });
    fixture.detectChanges();
    const [block]=await fixture.getDeferBlocks();await block.render(DeferBlockState.Complete);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-header,app-footer')).toBeNull();
    expect(fixture.nativeElement.querySelector('.whatsapp-float,.chatbot-wrapper')).toBeNull();
    expect(fixture.nativeElement.querySelector('.public-header-slot')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('main').getAttribute('aria-busy')).toBe('true');
    expect(fixture.componentInstance.contenidoPendiente()).toBe(true);
    ready.set(true);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-header')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-footer')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.whatsapp-float')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.chatbot-wrapper')).not.toBeNull();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
    expect(fixture.nativeElement.querySelector('main').hasAttribute('inert')).toBe(false);
  });
  it('does not load tenant CSS on marketing',()=>{
    const ensure=vi.fn(()=>Promise.resolve());
    TestBed.overrideProvider(TENANT_PUBLIC_STYLES,{useValue:{ready:signal(false),ensure}});
    const fixture=TestBed.createComponent(App);fixture.detectChanges();
    expect(ensure).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('app-marketing-nav')).not.toBeNull();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
  });

  it('should render the application shell', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-marketing-nav')).toBeTruthy();
    expect(compiled.querySelector('app-header')).toBeNull();
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
    expect(compiled.querySelector('app-footer')).toBeTruthy();
    expect(await fixture.getDeferBlocks()).toHaveLength(0);
  });
  it.each(['slug','host propio'])('carga el header del tenant por separado en %s sin mostrar chrome de marketing',async context=>{
    if(context==='slug')vi.spyOn(TestBed.inject(Location),'path').mockReturnValue('/e/x');
    else vi.spyOn(TestBed.inject(PlatformHostService),'isCustomTenantHost').mockReturnValue(true);
    const fixture=TestBed.createComponent(App);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.public-header-slot')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-header')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-marketing-nav')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-footer')).toBeNull();
    loadTenant(); fixture.detectChanges();
    const blocks=await fixture.getDeferBlocks();expect(blocks).toHaveLength(1);
    await blocks[0].render(DeferBlockState.Complete);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-header')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.public-header-slot')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-marketing-nav')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-footer')).not.toBeNull();
    TestBed.inject(HttpTestingController).expectNone(r=>r.url.includes('/publico/plataforma/soporte'));
  });
  it.each(['/acceso','/admin/resumen'])('no carga chrome público en %s',async path=>{
    vi.spyOn(TestBed.inject(Location),'path').mockReturnValue(path);
    const fixture=TestBed.createComponent(App);fixture.detectChanges();
    expect(await fixture.getDeferBlocks()).toHaveLength(0);
    expect(fixture.nativeElement.querySelector('app-header,app-marketing-nav,app-footer')).toBeNull();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
  });
  it('monta el footer del negocio junto con el Header cuando el contexto está disponible',async()=>{
    vi.spyOn(TestBed.inject(PlatformHostService),'isCustomTenantHost').mockReturnValue(true);
    const fixture=TestBed.createComponent(App);fixture.detectChanges();
    TestBed.inject(TenantContextService).setTenant({
      empresaId:42,slug:'negocio-prueba',nombreComercial:'Negocio de prueba',publicado:true,
      logoUrl:null,descripcionCorta:null,colorPrimario:null,colorSecundario:null,fuenteTitulos:null,fuenteCuerpo:null,
      heroTitulo:null,heroSubtitulo:null,heroImagenUrl:null,whatsapp:null,telefono:null,correo:null,direccion:null,
      instagramUrl:null,facebookUrl:null,tema:null,
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-footer')).toBeNull();
    const [block] = await fixture.getDeferBlocks();
    await block.render(DeferBlockState.Complete); fixture.detectChanges(); await fixture.whenStable();
    const footer=fixture.nativeElement.querySelector('app-footer') as HTMLElement;
    expect(footer).not.toBeNull();expect(footer.querySelector('.site-footer--marketing')).toBeNull();
  });

  it('no pinta navegación Fluora mientras espera el contexto del negocio', async () => {
    vi.spyOn(TestBed.inject(Location), 'path').mockReturnValue('/e/negocio-prueba/servicios');
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    const [block] = await fixture.getDeferBlocks();
    await block.render(DeferBlockState.Complete); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-header')).toBeNull();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(true);
    loadTenant(); fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.site-header--fluora-home')).toBeNull();
    expect(fixture.nativeElement.querySelector('.site-header--tenant')).not.toBeNull();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
  });

  it.each([true, false])('revela el contenido si falla el chunk del Header (negocio activo: %s)', async active => {
    vi.spyOn(TestBed.inject(Location), 'path').mockReturnValue('/e/negocio-prueba/servicios');
    if (active) loadTenant();
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    const [block] = await fixture.getDeferBlocks();
    await block.render(DeferBlockState.Error); fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-header')).toBeNull();
    expect(fixture.nativeElement.querySelector('.public-header-error a').getAttribute('href')).toBe(active ? '/e/negocio-prueba' : '/');
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
    expect(fixture.nativeElement.querySelector('main').hasAttribute('inert')).toBe(false);
  });

  it.each([NavigationCancellationCode.Redirect, NavigationCancellationCode.SupersededByNewNavigation])('espera la navegación siguiente tras una cancelación transitoria (%s)', code => {
    vi.spyOn(TestBed.inject(Location), 'path').mockReturnValue('/e/inexistente');
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    const events = TestBed.inject(Router).events as Subject<unknown>;
    events.next(new NavigationCancel(1, '/e/inexistente', 'Otra navegación continúa', code));
    fixture.detectChanges();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(true);
    expect(fixture.componentInstance.navegacionResuelta()).toBe(false);
    events.next(new NavigationEnd(2, '/', '/')); fixture.detectChanges();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
  });

  it('crea Header diferido al entrar desde marketing y conserva el outlet mientras carga', async () => {
    const router = TestBed.inject(Router);
    router.resetConfig([{ path: 'e/:slug/servicios', component: PublicRouteProbe }, { path: '', component: PublicRouteProbe }]);
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    await router.navigateByUrl('/'); fixture.detectChanges();
    expect(await fixture.getDeferBlocks()).toHaveLength(0);
    loadTenant('a'); await router.navigateByUrl('/e/a/servicios'); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#route-content')).not.toBeNull();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(true);
    const [block] = await fixture.getDeferBlocks();
    await block.render(DeferBlockState.Complete); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
    expect(fixture.nativeElement.querySelector('.site-header--tenant')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.site-header--fluora-home')).toBeNull();
  });

  it.each([
    new NavigationError(1, '/e/inexistente', new Error('No disponible')),
    new NavigationCancel(1, '/e/inexistente', 'No disponible'),
    new NavigationEnd(1, '/e/inexistente', '/e/inexistente')
  ])('no bloquea una navegación resuelta sin contexto ($constructor.name)', event => {
    vi.spyOn(TestBed.inject(Location), 'path').mockReturnValue('/e/inexistente');
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(true);
    (TestBed.inject(Router).events as Subject<unknown>).next(event);
    fixture.detectChanges();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
  });

  it('mantiene vivo el outlet pendiente, conserva Header entre negocios y lo retira al salir', async () => {
    vi.spyOn(TestBed.inject(Location), 'path').mockReturnValue('/e/a/servicios');
    const tenant = loadTenant('a'), router = TestBed.inject(Router);
    router.resetConfig([{ path: 'e/:slug/servicios', component: PublicRouteProbe }, { path: '', component: PublicRouteProbe }]);
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    await router.navigateByUrl('/e/a/servicios'); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#route-content')).not.toBeNull();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(true);
    const [block] = await fixture.getDeferBlocks();
    await block.render(DeferBlockState.Complete); fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    const header = fixture.debugElement.query(By.directive(HeaderComponent)).componentInstance;
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
    loadTenant('b'); await router.navigateByUrl('/e/b/servicios'); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.debugElement.query(By.directive(HeaderComponent)).componentInstance).toBe(header);
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
    await router.navigateByUrl('/'); fixture.detectChanges(); await fixture.whenStable();
    expect(tenant.activo()).toBe(false);
    expect(fixture.nativeElement.querySelector('app-header')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-marketing-nav')).not.toBeNull();
    expect(fixture.componentInstance.contenidoPendiente()).toBe(false);
  });

});
