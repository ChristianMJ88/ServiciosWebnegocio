import { Component, PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, vi } from 'vitest';
import { MarketingSectionsDirective } from './marketing-sections.directive';
import { MarketingSectionsService } from './marketing-sections.service';
@Component({standalone:true,imports:[MarketingSectionsDirective],template:'<div appMarketingSections id="inicio" style="scroll-margin-top:78px"><section id="como-funciona"></section><section id="producto"></section><section id="planes"></section><section id="faq"></section></div>'})
class Host {}
describe('sección activa de marketing',()=>{
 afterEach(()=>{vi.restoreAllMocks();vi.unstubAllGlobals();});
 it('contrato de servidor simulado: no crea observador ni programa RAF al redimensionar',()=>{
  const observer=vi.fn(()=>{throw Error('Server scrollspy accessed IO');});
  vi.stubGlobal('IntersectionObserver',observer);
  TestBed.configureTestingModule({imports:[Host],providers:[{provide:PLATFORM_ID,useValue:'server'}]});
  const fixture=TestBed.createComponent(Host);fixture.detectChanges();
  const frame=vi.spyOn(window,'requestAnimationFrame');
  window.dispatchEvent(new Event('resize'));
  expect(observer).not.toHaveBeenCalled();expect(frame).not.toHaveBeenCalled();
  expect(TestBed.inject(MarketingSectionsService).active()).toBe('');fixture.destroy();
 });
 it('cambia la ubicación al cruzar el encabezado y libera observadores al salir',()=>{
  const observers:{notify:()=>void;disconnect:ReturnType<typeof vi.fn>;observe:ReturnType<typeof vi.fn>;options:IntersectionObserverInit}[]=[];
  vi.stubGlobal('IntersectionObserver',class{
    readonly disconnect=vi.fn();readonly observe=vi.fn();
    constructor(callback:()=>void,options:IntersectionObserverInit){observers.push({notify:callback,disconnect:this.disconnect,observe:this.observe,options});}
  });
  TestBed.configureTestingModule({imports:[Host]});
  const fixture=TestBed.createComponent(Host);fixture.detectChanges();
  const positions:Record<string,number>={inicio:-10,'como-funciona':300,producto:1000,planes:2000,faq:3000};
  for(const element of fixture.nativeElement.querySelectorAll('[id]'))vi.spyOn(element,'getBoundingClientRect').mockImplementation(()=>({top:positions[(element as HTMLElement).id],height:700}) as DOMRect);
  let resizeCallback:FrameRequestCallback|undefined;const frame=vi.spyOn(window,'requestAnimationFrame').mockImplementation(callback=>{resizeCallback=callback;return 123;});const cancel=vi.spyOn(window,'cancelAnimationFrame');
  const state=TestBed.inject(MarketingSectionsService);const observer=observers[0];
  observer.notify();expect(state.active()).toBe('inicio');
  positions['como-funciona']=-400;positions['producto']=10;observer.notify();expect(state.active()).toBe('producto');
  positions['producto']=-600;positions['planes']=10;observer.notify();expect(state.active()).toBe('planes');
  positions['como-funciona']=17;positions['producto']=8;positions['planes']=-100;observer.notify();expect(state.active()).toBe('como-funciona');
  expect(observer.observe).toHaveBeenCalledTimes(5);
  expect(observer.options.rootMargin).toBe(`-80px 0px -${innerHeight-81}px 0px`);
  (fixture.nativeElement.querySelector('#inicio') as HTMLElement).style.scrollMarginTop='90px';
  window.dispatchEvent(new Event('resize'));window.dispatchEvent(new Event('resize'));expect(frame).toHaveBeenCalledOnce();expect(observers.length).toBe(1);resizeCallback!(0);expect(observer.disconnect).toHaveBeenCalledOnce();expect(observers.length).toBe(2);expect(observers[1].options.rootMargin).toBe(`-92px 0px -${innerHeight-93}px 0px`);
  window.dispatchEvent(new Event('resize'));
  fixture.destroy();expect(observers[1].disconnect).toHaveBeenCalledOnce();expect(state.active()).toBe('');expect(cancel).toHaveBeenCalledWith(123);
  window.dispatchEvent(new Event('resize'));expect(observers.length).toBe(2);
 });
 it('no publica una ubicación ficticia si el observador no está disponible',()=>{
  vi.stubGlobal('IntersectionObserver',undefined);TestBed.configureTestingModule({imports:[Host]});const fixture=TestBed.createComponent(Host);fixture.detectChanges();expect(TestBed.inject(MarketingSectionsService).active()).toBe('');
 });
});
