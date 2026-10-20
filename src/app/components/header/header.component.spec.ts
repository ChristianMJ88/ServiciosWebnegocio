import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { HeaderComponent } from './header.component';
import { PlatformHostService } from '../../core/platform/platform-host.service';

describe('Header: identidad desde la ruta inicial', () => {
  it.each(['/e/negocio-prueba', '/e/negocio-prueba?campana=prueba#inicio', '/'])('no pinta Fluora al entrar directamente en %s', async path => {
    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
    vi.spyOn(TestBed.inject(Location), 'path').mockReturnValue(path);
    if (path === '/') vi.spyOn(TestBed.inject(PlatformHostService), 'isCustomTenantHost').mockReturnValue(true);
    TestBed.inject(TenantContextService).setTenant({
      empresaId:42,slug:'negocio-prueba',nombreComercial:'Una marca con nombre largo',publicado:true,
      logoUrl:null,descripcionCorta:null,colorPrimario:null,colorSecundario:null,fuenteTitulos:null,fuenteCuerpo:null,
      heroTitulo:null,heroSubtitulo:null,heroImagenUrl:null,whatsapp:null,telefono:null,correo:null,direccion:null,
      instagramUrl:null,facebookUrl:null,tema:null,
    });
    expect(TestBed.inject(Router).url).toBe('/');
    const fixture = TestBed.createComponent(HeaderComponent); fixture.detectChanges();
    // routeFor conserva /e/slug incluso en host propio; esta tarea no cambia ese contrato.
    const isHome = path !== '/';
    expect(fixture.componentInstance.homeLink()).toBe('/e/negocio-prueba');
    expect(fixture.componentInstance.tenantHomeActiva()).toBe(isHome);
    expect(fixture.nativeElement.querySelector('.site-header--fluora-home')).toBeNull();
    expect(!!fixture.nativeElement.querySelector('.site-header--over-hero')).toBe(isHome);
    expect(fixture.nativeElement.textContent).toContain('Una marca con nombre largo');
    expect(fixture.nativeElement.querySelector('.brand-initials')).not.toBeNull();
  });
});
