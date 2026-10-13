import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { SitePromotionsComponent } from './site-promotions.component';
import { SitePromotionsFacade } from './site-promotions.facade';

describe('Editor de promociones', () => {
  it('sends the existing promotion ID when saving edits', async () => {
    const guardar=vi.fn((_payload: any) => of([]));
    await TestBed.configureTestingModule({imports:[SitePromotionsComponent]})
      .overrideComponent(SitePromotionsComponent,{set:{providers:[{provide:SitePromotionsFacade,useValue:{
        cargar:()=>of({promociones:[],servicios:[],sucursales:[],media:null}),guardar
      }}]}}).compileComponents();
    const fixture=TestBed.createComponent(SitePromotionsComponent);fixture.detectChanges();
    const editor=fixture.componentInstance;editor.agregar();editor.promociones()[0].id=42;editor.promociones()[0].titulo='Oferta existente';
    editor.guardar();
    expect(guardar.mock.calls[0][0].promociones[0].id).toBe(42);
  });
  it('expande el editor con fechas sin ciclos de actualización y conserva los cambios al contraerlo', async () => {
    await TestBed.configureTestingModule({ imports: [SitePromotionsComponent] })
      .overrideComponent(SitePromotionsComponent, { set: { providers: [
        { provide: SitePromotionsFacade, useValue: {
          cargar: () => of({ promociones: [], servicios: [], sucursales: [], media: null })
        } }
      ] } }).compileComponents();
    const fixture = TestBed.createComponent(SitePromotionsComponent);
    fixture.detectChanges();
    const editor = fixture.componentInstance;
    editor.agregar();
    const item = editor.promociones()[0];
    item.fechaInicio = '2026-10-07';
    item.fechaFin = '2026-10-31';
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.promotion-card__fields')).toBeTruthy();
    expect(editor.fechaComoDate(item.fechaInicio)).toBe(editor.fechaComoDate(item.fechaInicio));
    item.titulo = 'Oferta editada';
    editor.alternar(item, false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.promotion-card__fields')).toBeNull();
    editor.alternar(item, true);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('input[name="promoTitulo0"]').value).toBe('Oferta editada');
  });
});
