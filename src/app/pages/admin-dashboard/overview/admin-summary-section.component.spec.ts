import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import esMx from '@angular/common/locales/es-MX';
import { describe, expect, it } from 'vitest';
import { AdminSummarySectionComponent } from './admin-summary-section.component';
import { ResumenAdmin } from '../../../core/admin/admin.service';

describe('Finanzas de citas en el resumen', () => {
  it('presenta importes backend por moneda, deuda histórica y custodia no disponible', () => {
    registerLocaleData(esMx);
    TestBed.configureTestingModule({imports:[AdminSummarySectionComponent]});
    const fixture=TestBed.createComponent(AdminSummarySectionComponent);
    fixture.componentRef.setInput('resumen',{
      totalCitas:1,pendientes:0,confirmadas:0,finalizadas:1,canceladas:0,noAsistio:0,citasHoy:0,
      ingresosProgramados:999,ingresosFinalizados:999,
      puedeVerFinanzas:true, finanzasCitas:[{moneda:'MXN',ingresosProgramados:500,ingresosDevengados:500,ingresosCobrados:100,
        saldoPorCobrar:1400,anticiposEnCustodia:null,ingresosAnticiposVencidos:null,custodiaDisponible:false}]
    } satisfies ResumenAdmin);
    fixture.detectChanges();
    const finanzas=fixture.nativeElement.querySelector('.dashboard-finances') as HTMLElement;
    expect(finanzas.textContent).toContain('MXN');
    expect(finanzas.textContent).toContain('$500.00');
    expect(finanzas.textContent).toContain('$100.00');
    expect(finanzas.textContent).toContain('$1,400.00');
    expect(finanzas.textContent).toContain('más de 90 días');
    expect(finanzas.textContent).toContain('aún no está disponible');
    expect(finanzas.textContent).not.toContain('$999');
    fixture.componentRef.setInput('resumen',{puedeVerFinanzas:false,finanzasCitas:[{moneda:'MXN',saldoPorCobrar:1400}]} as ResumenAdmin);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.dashboard-finances')).toBeNull();
  });
});
