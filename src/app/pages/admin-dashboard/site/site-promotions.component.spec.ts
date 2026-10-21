import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { Observable, of } from 'rxjs';
import { beforeEach, describe, expect, it, Mock, vi } from 'vitest';
import { SitePromotionsComponent } from './site-promotions.component';
import { SitePromotionsFacade } from './site-promotions.facade';

describe('Editor de promociones', () => {
  let fixture: ComponentFixture<SitePromotionsComponent>;
  let guardar: Mock<(payload: any) => Observable<never[]>>;
  beforeEach(async () => {
    guardar=vi.fn((_payload: any) => of([]));
    TestBed.configureTestingModule({imports:[SitePromotionsComponent]});
    // Patch only DI: overriding component metadata would JIT-compile the whole template.
    TestBed.overrideProvider(SitePromotionsFacade,{useValue:{
        cargar:()=>of({promociones:[],servicios:[],sucursales:[],media:null}),guardar
      }});
    await TestBed.compileComponents();
    fixture=TestBed.createComponent(SitePromotionsComponent);fixture.detectChanges();
    await fixture.whenStable();
  });
  it('sends the existing promotion ID when saving edits', () => {
    const editor=fixture.componentInstance;editor.agregar();editor.promociones()[0].id=42;editor.promociones()[0].titulo='Oferta existente';
    editor.guardar();
    expect(guardar).toHaveBeenCalledOnce();
    expect(guardar.mock.calls[0][0].promociones[0].id).toBe(42);
  });
  it('expande el editor con fechas sin ciclos de actualización y conserva los cambios al contraerlo', async () => {
    expect(fixture.debugElement.injector.get(MAT_DATE_LOCALE)).toBe('es-MX');
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
