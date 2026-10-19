import { DeferBlockBehavior, DeferBlockState, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { Location } from '@angular/common';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlatformHostService } from './core/platform/platform-host.service';
import { vi } from 'vitest';
import { App } from './app';
import { TenantContextService } from './core/tenant/tenant-context.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      deferBlockBehavior: DeferBlockBehavior.Manual,
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
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
    const blocks=await fixture.getDeferBlocks();expect(blocks).toHaveLength(1);
    await blocks[0].render(DeferBlockState.Complete);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-header')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.public-header-slot')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-marketing-nav')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-footer')).toBeNull();
    TestBed.inject(HttpTestingController).expectNone(r=>r.url.includes('/publico/plataforma/soporte'));
  });
  it.each(['/acceso','/admin/resumen'])('no carga chrome público en %s',async path=>{
    vi.spyOn(TestBed.inject(Location),'path').mockReturnValue(path);
    const fixture=TestBed.createComponent(App);fixture.detectChanges();
    expect(await fixture.getDeferBlocks()).toHaveLength(0);
    expect(fixture.nativeElement.querySelector('app-header,app-marketing-nav,app-footer')).toBeNull();
  });
  it('monta el footer del negocio cuando el contexto del resolver está disponible',()=>{
    vi.spyOn(TestBed.inject(PlatformHostService),'isCustomTenantHost').mockReturnValue(true);
    const fixture=TestBed.createComponent(App);fixture.detectChanges();
    TestBed.inject(TenantContextService).setTenant({
      empresaId:42,slug:'negocio-prueba',nombreComercial:'Negocio de prueba',publicado:true,
      logoUrl:null,descripcionCorta:null,colorPrimario:null,colorSecundario:null,fuenteTitulos:null,fuenteCuerpo:null,
      heroTitulo:null,heroSubtitulo:null,heroImagenUrl:null,whatsapp:null,telefono:null,correo:null,direccion:null,
      instagramUrl:null,facebookUrl:null,tema:null,
    });
    fixture.detectChanges();
    const footer=fixture.nativeElement.querySelector('app-footer') as HTMLElement;
    expect(footer).not.toBeNull();expect(footer.querySelector('.site-footer--marketing')).toBeNull();
  });

});
