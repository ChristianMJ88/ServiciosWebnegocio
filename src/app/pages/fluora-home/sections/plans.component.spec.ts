import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HomePlansComponent } from './plans.component';
import { MarketingOfferService } from '../../../core/marketing/marketing-offer.service';
import { PlatformHostService } from '../../../core/platform/platform-host.service';
import { environment } from '../../../../environments/environment';
import { afterEach,vi } from 'vitest';
describe('planes comerciales del inicio',()=>{
 afterEach(()=>vi.unstubAllGlobals());
 function setup(){TestBed.configureTestingModule({imports:[HomePlansComponent],providers:[provideHttpClient(),provideHttpClientTesting()]});vi.spyOn(TestBed.inject(PlatformHostService),'appUrl').mockImplementation(path=>'https://app.example.test'+path);const offer=TestBed.inject(MarketingOfferService);offer.load();const fixture=TestBed.createComponent(HomePlansComponent);fixture.detectChanges();return {fixture,offer,http:TestBed.inject(HttpTestingController)};}
 const plan={id:93,nombre:'Oferta editable',descripcion:'Descripción desde el servidor',moneda:'MXN',precioMensual:287.5,destacado:true,diasPrueba:17,modulos:[{moduloId:701,codigo:'PRUEBA',nombre:'Módulo del servidor',limiteUso:null}],limites:[{limiteId:501,nombre:'Capacidad editable',codigo:'OTRA',valor:19,unidad:'elementos'}]};
 it('presenta precio, prueba, módulos, límites e id del backend sin recalcular',()=>{const {fixture,http}=setup();http.expectOne(environment.apiBaseUrl+'/publico/plataforma/planes').flush([plan]);fixture.detectChanges();const card=fixture.nativeElement.querySelector('article');expect(card.textContent).toContain('$287.50');expect(card.textContent).toContain('17 días');expect(card.textContent).toContain('Módulo del servidor');expect(card.textContent).toContain('19 elementos');expect(card.querySelector('a').href).toContain('https://app.example.test/registro?plan=93');expect(card.querySelector('details').open).toBe(false);http.verify();});
 it('no ofrece una prueba si el servidor no la configura',()=>{const {fixture,http}=setup();http.expectOne(environment.apiBaseUrl+'/publico/plataforma/planes').flush([{...plan,diasPrueba:0}]);fixture.detectChanges();expect(fixture.nativeElement.querySelector('aside')).toBeNull();expect(fixture.nativeElement.querySelector('article a').textContent).toContain('Comenzar con Oferta editable');http.verify();});
 it('permite consultar otra vez tras un error sin inventar precios',()=>{const {fixture,http}=setup();expect(fixture.nativeElement.querySelector('.fl-home-rail-controls')).toBeNull();http.expectOne(environment.apiBaseUrl+'/publico/plataforma/planes').flush({}, {status:503,statusText:'Unavailable'});fixture.detectChanges();expect(fixture.nativeElement.querySelector('article')).toBeNull();expect(fixture.nativeElement.querySelector('.fl-home-rail-controls')).toBeNull();const retry=[...fixture.nativeElement.querySelectorAll('button')].find((button:any)=>button.textContent.includes('Volver a consultar')) as HTMLButtonElement;retry.click();http.expectOne(environment.apiBaseUrl+'/publico/plataforma/planes').flush([plan]);fixture.detectChanges();expect(fixture.nativeElement.querySelector('article').textContent).toContain('$287.50');http.verify();});
 it('solo anuncia geometría lista tras medir y deja de estar lista durante una nueva carga',async()=>{
  let notify=()=>{};vi.stubGlobal('ResizeObserver',class{constructor(callback:ResizeObserverCallback){notify=()=>callback([],this as unknown as ResizeObserver);}observe(){}disconnect(){}});
  const {fixture,http,offer}=setup();expect(fixture.componentInstance.layoutReady()).toBe(false);http.expectOne(environment.apiBaseUrl+'/publico/plataforma/planes').flush([plan]);fixture.detectChanges();expect(fixture.componentInstance.layoutReady()).toBe(false);
  notify();await fixture.whenStable();expect(fixture.componentInstance.layoutReady()).toBe(true);
  // The request owns loading; this fixture drives its public presentation signal.
  offer.loading.set(true);fixture.detectChanges();expect(fixture.componentInstance.layoutReady()).toBe(false);expect(fixture.nativeElement.querySelector('.fl-home-plans-rail')).toBeNull();
  offer.loading.set(false);fixture.detectChanges();expect(fixture.componentInstance.layoutReady()).toBe(false);notify();await fixture.whenStable();expect(fixture.componentInstance.layoutReady()).toBe(true);http.verify();
 });
});
