import { DeferBlockBehavior, DeferBlockState } from '@angular/core/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { of,Subject } from 'rxjs';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { FluoraHomeComponent } from './fluora-home.component';
import { PlatformHostService } from '../../core/platform/platform-host.service';
import { environment } from '../../../environments/environment';
import { Meta } from '@angular/platform-browser';
import { afterEach, vi } from 'vitest';
import { MarketingOfferService } from '../../core/marketing/marketing-offer.service';
import { LandingMarketingService } from '../../core/marketing/landing-marketing.service';
import { PlatformContactService } from '../../core/marketing/platform-contact.service';
import { PublicPlansService, PlanPublicoFluora } from '../../core/platform/public-plans.service';
const animation=vi.hoisted(()=>({loaded:vi.fn(),to:vi.fn(),revert:vi.fn()}));
vi.mock('gsap',()=>{animation.loaded();return {gsap:{context:(callback:()=>void)=>{callback();return {revert:animation.revert};},to:animation.to}};});
const originalMotion=FluoraHomeComponent.prototype.ngAfterViewInit;
const base=environment.apiBaseUrl;
const anchorPlan:PlanPublicoFluora={id:992,codigo:'anchor-fixture',nombre:'Plan de prueba',descripcion:null,moneda:'MXN',precioMensual:123,precioAnual:null,destacado:true,diasPrueba:0,diasGracia:0,vigenteDesde:null,vigenteHasta:null,modulos:[],limites:[]};
describe('inicio de Fluora',()=>{
 afterEach(()=>vi.unstubAllGlobals());
 async function setup(custom=false,fragment:string|null=null,holdPlans=false,fragments?:Subject<string|null>){vi.stubGlobal('IntersectionObserver',class{observe(){}unobserve(){}disconnect(){}});TestBed.configureTestingModule({imports:[FluoraHomeComponent],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),...(fragment||fragments?[{provide:ActivatedRoute,useValue:{fragment:fragments??of(fragment)}}]:[])],deferBlockBehavior:DeferBlockBehavior.Manual});await TestBed.compileComponents();vi.spyOn(TestBed.inject(PlatformHostService),'isCustomTenantHost').mockReturnValue(custom);vi.spyOn(FluoraHomeComponent.prototype,'ngAfterViewInit').mockImplementation(async()=>{});const meta=TestBed.inject(Meta),beforeTheme=meta.getTag('name="theme-color"')?.content??null;const fixture=TestBed.createComponent(FluoraHomeComponent);fixture.detectChanges();const http=TestBed.inject(HttpTestingController);if(!holdPlans)http.expectOne(base+'/publico/plataforma/planes').flush([]);http.expectOne(base+'/publico/marketing/landings/navegacion').flush([]);return {fixture,http,meta,beforeTheme};}
 async function render(fixture:any){for(const block of await fixture.getDeferBlocks())await block.render(DeferBlockState.Complete);fixture.detectChanges();}
 function railObserver(){const callbacks=new Map<Element,()=>void>();vi.stubGlobal('ResizeObserver',class{constructor(private readonly callback:ResizeObserverCallback){}observe(target:Element){callbacks.set(target,()=>this.callback([],this as unknown as ResizeObserver));}disconnect(){}});return(target:Element)=>{const callback=callbacks.get(target);expect(callback).toBeDefined();callback!();};}
 it('compone el horario y el contacto desde el backend',async()=>{const {fixture,http}=await setup();http.expectOne(base+'/publico/plataforma/soporte').flush({correo:'help@example.test',telefono:'+12025550189',dias:[2,4],horaInicio:'10:00',horaFin:'16:00',zonaHoraria:'Europe/Madrid'});await render(fixture);expect(fixture.nativeElement.querySelector('#faq').textContent).toContain('martes y jueves');expect(fixture.nativeElement.querySelector('#faq a[href="mailto:help@example.test"]')).not.toBeNull();http.expectNone(base+'/publico/plataforma/contacto-legal');http.verify();});
 it('oculta la pregunta si el soporte no está publicado',async()=>{const {fixture,http}=await setup();http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});await render(fixture);expect(fixture.nativeElement.querySelector('#faq').textContent).not.toContain('¿Cómo puedo contactar con soporte?');http.verify();});
 it('no solicita soporte en un host propio',async()=>{const {fixture,http}=await setup(true);await render(fixture);http.expectNone(base+'/publico/plataforma/soporte');expect(fixture.nativeElement.querySelector('#faq').textContent).not.toContain('¿Cómo puedo contactar con soporte?');http.verify();});
 it('libera la apariencia temporal y no altera viewport desde Home',async()=>{const {fixture,http,meta,beforeTheme}=await setup();http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});const viewport=meta.getTag('name="viewport"')?.content;expect(meta.getTag('name="theme-color"')?.content).toBe('#0A0F1A');fixture.destroy();expect(meta.getTag('name="theme-color"')?.content??null).toBe(beforeTheme);expect(meta.getTag('name="viewport"')?.content).toBe(viewport);http.verify();});
 it('Home no activa apariencia de plataforma en un host propio',async()=>{const {fixture,http,meta,beforeTheme}=await setup(true);expect(meta.getTag('name="theme-color"')?.content??null).toBe(beforeTheme);fixture.destroy();http.verify();});
 it('espera a la oferta y a los componentes diferidos antes de posicionar una URL con fragmento',async()=>{const {fixture,http}=await setup(false,'planes',true);const target=fixture.nativeElement.querySelector('#planes') as HTMLElement;const scroll=vi.fn();target.scrollIntoView=scroll;http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});await render(fixture);expect(scroll).not.toHaveBeenCalled();http.expectOne(base+'/publico/plataforma/planes').flush([]);fixture.detectChanges();await fixture.whenStable();expect(scroll).toHaveBeenCalledWith({block:'start',behavior:'auto'});fixture.detectChanges();await fixture.whenStable();expect(scroll).toHaveBeenCalledOnce();http.verify();});
 it('espera a los cuatro diferidos aunque las APIs ya respondieron',async()=>{const {fixture,http}=await setup(false,'planes');const target=fixture.nativeElement.querySelector('#planes') as HTMLElement;const scroll=vi.fn();target.scrollIntoView=scroll;http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});fixture.detectChanges();await fixture.whenStable();expect(scroll).not.toHaveBeenCalled();await render(fixture);await fixture.whenStable();expect(scroll).toHaveBeenCalledOnce();http.verify();});
 for(const overflow of [true,false])it('posiciona FAQ después de medir el rail '+(overflow?'con':'sin')+' overflow, sin detección manual',async()=>{
  const notify=railObserver(),{fixture,http}=await setup(false,'faq',true);const target=fixture.nativeElement.querySelector('#faq') as HTMLElement;
  const seen:boolean[]=[],scroll=vi.fn(()=>seen.push(!!fixture.nativeElement.querySelector('.fl-home-rail-controls')));target.scrollIntoView=scroll;
  http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});http.expectOne(base+'/publico/plataforma/planes').flush([anchorPlan]);await render(fixture);await fixture.whenStable();
  const rail=fixture.nativeElement.querySelector('.fl-home-plans-rail') as HTMLElement;Object.defineProperties(rail,{clientWidth:{configurable:true,value:320},scrollWidth:{configurable:true,value:overflow?640:320}});
  expect(scroll).not.toHaveBeenCalled();notify(rail);await fixture.whenStable();expect(scroll).toHaveBeenCalledOnce();expect(seen).toEqual([overflow]);
  Object.defineProperty(rail,'scrollWidth',{configurable:true,value:overflow?320:640});notify(rail);await fixture.whenStable();expect(scroll).toHaveBeenCalledOnce();http.verify();
 });
 it('conserva el fallback de medición sin ResizeObserver',async()=>{
  vi.stubGlobal('ResizeObserver',undefined);const {fixture,http}=await setup(false,'faq',true);const target=fixture.nativeElement.querySelector('#faq') as HTMLElement,scroll=vi.fn();target.scrollIntoView=scroll;
  http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});http.expectOne(base+'/publico/plataforma/planes').flush([anchorPlan]);await render(fixture);await fixture.whenStable();expect(scroll).toHaveBeenCalledOnce();http.verify();
 });
 it('atiende un fragmento posterior y otro distinto sin forzar una pasada de detección',async()=>{
  const fragments=new Subject<string|null>(),{fixture,http}=await setup(false,null,false,fragments),faq=vi.fn(),plans=vi.fn();fixture.nativeElement.querySelector('#faq').scrollIntoView=faq;fixture.nativeElement.querySelector('#planes').scrollIntoView=plans;
  http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});await render(fixture);await fixture.whenStable();expect(faq).not.toHaveBeenCalled();
  fragments.next('faq');await fixture.whenStable();expect(faq).toHaveBeenCalledOnce();expect(plans).not.toHaveBeenCalled();
  fragments.next('planes');await fixture.whenStable();expect(plans).toHaveBeenCalledOnce();expect(faq).toHaveBeenCalledOnce();http.verify();
 });
 it('posiciona FAQ con planes fallidos sin esperar un rail que no existe',async()=>{
  const {fixture,http}=await setup(false,'faq',true);const target=fixture.nativeElement.querySelector('#faq') as HTMLElement,scroll=vi.fn();target.scrollIntoView=scroll;
  http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});http.expectOne(base+'/publico/plataforma/planes').flush({}, {status:503,statusText:'Unavailable'});await render(fixture);await fixture.whenStable();expect(scroll).toHaveBeenCalledOnce();expect(fixture.nativeElement.querySelector('.fl-home-plans-rail')).toBeNull();http.verify();
 });
 it('un gesto antes de medir cancela el ancla y no vuelve a posicionar al redimensionar',async()=>{
  const notify=railObserver(),{fixture,http}=await setup(false,'faq',true);const target=fixture.nativeElement.querySelector('#faq') as HTMLElement,scroll=vi.fn();target.scrollIntoView=scroll;
  http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});http.expectOne(base+'/publico/plataforma/planes').flush([anchorPlan]);await render(fixture);await fixture.whenStable();window.dispatchEvent(new Event('wheel'));
  notify(fixture.nativeElement.querySelector('.fl-home-plans-rail'));await fixture.whenStable();expect(scroll).not.toHaveBeenCalled();http.verify();
 });
 it('el hero nunca queda ocupado y un gesto cancela el ancla pendiente',async()=>{const {fixture,http}=await setup(false,'planes',true);const target=fixture.nativeElement.querySelector('#planes') as HTMLElement;const scroll=vi.fn();target.scrollIntoView=scroll;expect(fixture.nativeElement.querySelector('app-home-hero').closest('[aria-busy="true"]')).toBeNull();window.dispatchEvent(new Event('wheel'));http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});await render(fixture);http.expectOne(base+'/publico/plataforma/planes').flush([]);fixture.detectChanges();await fixture.whenStable();expect(scroll).not.toHaveBeenCalled();http.verify();});
 it('no importa ni anima GSAP en móvil o con movimiento reducido',async()=>{const {fixture,http}=await setup();http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});const load=vi.spyOn(fixture.componentInstance as unknown as {loadAnimation:()=>Promise<unknown>},'loadAnimation');animation.to.mockClear();for(const [desktop,reduced] of [[false,false],[true,true]]){vi.stubGlobal('matchMedia',(query:string)=>({matches:query.includes('reduced-motion')?reduced:desktop}));await originalMotion.call(fixture.componentInstance);}expect(load).not.toHaveBeenCalled();expect(animation.to).not.toHaveBeenCalled();http.verify();});
 it('carga una animación decorativa en escritorio y la revierte al destruir',async()=>{const {fixture,http}=await setup();http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});vi.stubGlobal('matchMedia',(query:string)=>({matches:!query.includes('reduced-motion')}));const load=vi.spyOn(fixture.componentInstance as unknown as {loadAnimation:()=>Promise<unknown>},'loadAnimation');animation.to.mockClear();animation.revert.mockClear();await originalMotion.call(fixture.componentInstance);expect(load).toHaveBeenCalledOnce();expect(animation.to).toHaveBeenCalledOnce();expect(animation.to.mock.calls[0][0]).toBe('.fl-home-preview-icon');fixture.destroy();expect(animation.revert).toHaveBeenCalledOnce();http.verify();});
 it('conserva anclas antiguas y cuatro bloques diferidos sin crear otro main',async()=>{const {fixture,http}=await setup();http.expectOne(base+'/publico/plataforma/soporte').flush({}, {status:404,statusText:'Not found'});expect((await fixture.getDeferBlocks()).length).toBe(4);expect(fixture.nativeElement.querySelector('main')).toBeNull();for(const id of ['inicio','beneficios','como-funciona','demostracion','producto','planes','faq'])expect(fixture.nativeElement.querySelector('#'+id)).not.toBeNull();http.verify();});
 it('mantiene la primera vista sin datos aunque oferta y navegación ya estén en caché',async()=>{
  const plan:PlanPublicoFluora={id:991,codigo:'fixture',nombre:'Oferta de prueba',descripcion:null,moneda:'MXN',precioMensual:123,precioAnual:null,destacado:true,diasPrueba:11,diasGracia:2,vigenteDesde:null,vigenteHasta:null,modulos:[],limites:[]};
  vi.stubGlobal('IntersectionObserver',class{observe(){}unobserve(){}disconnect(){}});
  TestBed.configureTestingModule({imports:[FluoraHomeComponent],deferBlockBehavior:DeferBlockBehavior.Manual,
    providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),
      {provide:PublicPlansService,useValue:{listar:()=>of([plan])}},
      {provide:LandingMarketingService,useValue:{navegacion:()=>of([{slug:'fixture',titulo:'Fluora para pruebas',descripcion:'Catálogo de prueba'}])}},
      {provide:PlatformContactService,useValue:{publicSupport:()=>of(null)}}]});
  await TestBed.compileComponents();
  const offer=TestBed.inject(MarketingOfferService);offer.load();expect(offer.recommended()).toBe(plan);
  vi.spyOn(FluoraHomeComponent.prototype,'ngAfterViewInit').mockImplementation(async()=>{});
  const fixture=TestBed.createComponent(FluoraHomeComponent);
  // A local view pass leaves afterNextRender for the scheduled application pass.
  fixture.changeDetectorRef.detectChanges();
  const hero=fixture.nativeElement.querySelector('app-home-hero') as HTMLElement;
  expect(fixture.componentInstance.hydrated()).toBe(false);
  expect(hero.querySelectorAll('.fl-home-hero-verticals a').length).toBe(0);
  expect(hero.querySelector('.fl-home-hero-actions .fl-button')?.textContent?.trim()).toBe(offer.fallbackLabel);
  expect(hero.querySelector('.fl-home-hero-reassurance')?.textContent).not.toContain('Prueba sin tarjeta');
  await fixture.whenStable();fixture.detectChanges();
  expect(fixture.componentInstance.hydrated()).toBe(true);
  expect(hero.querySelectorAll('.fl-home-hero-verticals a').length).toBe(1);
  expect(hero.querySelector('.fl-home-hero-actions .fl-button')?.textContent).toContain('Probar 11 días');
  expect(hero.querySelector('.fl-home-hero-reassurance')?.textContent).toContain('Prueba sin tarjeta');
  expect(fixture.componentInstance.dataReady()).toBe(true);
  TestBed.inject(HttpTestingController).verify();
 });
});
