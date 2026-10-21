import { TestBed } from '@angular/core/testing';
import { Meta } from '@angular/platform-browser';
import { MarketingAppearanceService } from './marketing-appearance.service';
describe('apariencia temporal de marketing',()=>{
 afterEach(()=>{const meta=TestBed.inject(Meta);meta.removeTag('name="theme-color"');meta.removeTag('name="viewport"');});
 it('activa safe area solo en marketing y restaura los valores previos',()=>{const meta=TestBed.inject(Meta);meta.updateTag({name:'theme-color',content:'#ffffff'});meta.updateTag({name:'viewport',content:'width=device-width, initial-scale=1'});const release=TestBed.inject(MarketingAppearanceService).activate(true);expect(meta.getTag('name="theme-color"')?.content).toBe('#0A0F1A');expect(meta.getTag('name="viewport"')?.content).toContain('viewport-fit=cover');release();expect(meta.getTag('name="theme-color"')?.content).toBe('#ffffff');expect(meta.getTag('name="viewport"')?.content).toBe('width=device-width, initial-scale=1');release();});
 it('comparte activación y retira metas que antes no existían solo al liberar la última vista',()=>{const meta=TestBed.inject(Meta);meta.removeTag('name="theme-color"');meta.removeTag('name="viewport"');const service=TestBed.inject(MarketingAppearanceService);const a=service.activate(true),b=service.activate(true);a();expect(meta.getTag('name="theme-color"')).not.toBeNull();b();expect(meta.getTag('name="theme-color"')).toBeNull();expect(meta.getTag('name="viewport"')).toBeNull();});
 it('la vista de Home sin navegación nativa no activa cover',()=>{const meta=TestBed.inject(Meta);meta.updateTag({name:'viewport',content:'width=device-width, initial-scale=1'});const release=TestBed.inject(MarketingAppearanceService).activate();expect(meta.getTag('name="viewport"')?.content).toBe('width=device-width, initial-scale=1');release();});
 it('normaliza viewport-fit al principio y restaura la cadena exacta original',()=>{const meta=TestBed.inject(Meta);const original='viewport-fit=auto, width=device-width, initial-scale=1';meta.updateTag({name:'viewport',content:original});const release=TestBed.inject(MarketingAppearanceService).activate(true);expect(meta.getTag('name="viewport"')?.content).toBe('width=device-width, initial-scale=1, viewport-fit=cover');release();expect(meta.getTag('name="viewport"')?.content).toBe(original);});
 it('recupera el original serializado y retira el tema creado por servidor al liberar la última vista',()=>{
  const meta=TestBed.inject(Meta),original='width=device-width, initial-scale=1';
  meta.updateTag({name:'theme-color',content:'#0A0F1A','data-fl-lease':'created'});
  meta.updateTag({name:'viewport',content:original+', viewport-fit=cover','data-fl-original':original});
  const service=TestBed.inject(MarketingAppearanceService),nav=service.activate(true),home=service.activate();
  home();expect(meta.getTag('name="theme-color"')).not.toBeNull();expect(meta.getTag('name="viewport"')?.content).toContain('viewport-fit=cover');
  nav();expect(meta.getTag('name="theme-color"')).toBeNull();expect(meta.getTag('name="viewport"')?.content).toBe(original);
  expect(meta.getTag('name="viewport"')?.hasAttribute('data-fl-original')).toBe(false);
 });
 it('distingue un original vacío de un tag creado durante el render del servidor',()=>{
  const meta=TestBed.inject(Meta);meta.updateTag({name:'theme-color',content:'#0A0F1A','data-fl-original':''});
  const release=TestBed.inject(MarketingAppearanceService).activate();release();
  expect(meta.getTag('name="theme-color"')?.content).toBe('');expect(meta.getTag('name="theme-color"')?.hasAttribute('data-fl-original')).toBe(false);
 });
 it('serializa el valor original o la creación del tag para el cliente posterior',()=>{
  const meta=TestBed.inject(Meta),service=TestBed.inject(MarketingAppearanceService);
  meta.updateTag({name:'theme-color',content:'#ffffff'});
  const existing=service.activate();
  expect(meta.getTag('name="theme-color"')?.getAttribute('data-fl-original')).toBe('#ffffff');
  expect(meta.getTag('name="theme-color"')?.hasAttribute('data-fl-lease')).toBe(false);existing();
  meta.removeTag('name="theme-color"');
  const created=service.activate();
  expect(meta.getTag('name="theme-color"')?.getAttribute('data-fl-lease')).toBe('created');
  expect(meta.getTag('name="theme-color"')?.hasAttribute('data-fl-original')).toBe(false);created();
 });

});
