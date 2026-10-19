import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { PlatformHostService } from '../platform/platform-host.service';
import { prepareMarketingHome } from './marketing-bootstrap';
describe('carga inicial aislada de marketing',()=>{
 function setup(path='/',custom=false,app=false){
  const page=document.implementation.createHTMLDocument();
  const browserDocument={head:page.head,createElement:page.createElement.bind(page),location:{pathname:path}};
  TestBed.configureTestingModule({providers:[{provide:DOCUMENT,useValue:browserDocument},{provide:PlatformHostService,useValue:{isCustomTenantHost:()=>custom,isAppHost:()=>app}}]});
  const load=vi.fn(()=>Promise.resolve({}));return {page,load,run:()=>TestBed.runInInjectionContext(()=>prepareMarketingHome(load))};
 }
 it('adelanta el módulo y las fuentes solo en la portada de plataforma',()=>{
  const {page,load,run}=setup();run();expect(load).toHaveBeenCalledOnce();const fonts=page.head.querySelectorAll<HTMLLinkElement>('link[rel="preload"]');expect(fonts.length).toBe(2);
  for(const font of fonts){expect(font.as).toBe('font');expect(font.type).toBe('font/woff2');expect(font.crossOrigin).toBe('anonymous');}
  run();expect(page.head.querySelectorAll('link[rel="preload"]').length).toBe(2);
 });
 it.each([['/e/otro-negocio',false,false],['/',true,false],['/',false,true],['/barberias',false,false]])('no añade bytes de Home ni fuentes a %s con hostCustom=%s/app=%s',(path,custom,app)=>{
  const {page,load,run}=setup(path as string,custom as boolean,app as boolean);run();expect(load).not.toHaveBeenCalled();expect(page.head.querySelector('link')).toBeNull();
 });
});
