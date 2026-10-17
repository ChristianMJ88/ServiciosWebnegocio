import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeEsMx from '@angular/common/locales/es-MX';
import { of } from 'rxjs';
import { describe, it, expect } from 'vitest';
import { CajaPaymentsSectionComponent } from './caja-payments-section.component';
import { CajaService, CitaPorCobrar } from '../../../core/caja/caja.service';
import { crearFormularioPago } from '../forms/caja.forms';

describe('Desglose pactado de promociones en Caja', () => {
  function render(descuento: number) {
    registerLocaleData(localeEsMx);
    TestBed.configureTestingModule({
      imports: [CajaPaymentsSectionComponent],
      providers: [{ provide: CajaService, useValue: { listarTerminalesPoint: () => of([]) } }]
    });
    const fixture = TestBed.createComponent(CajaPaymentsSectionComponent);
    const cita: CitaPorCobrar = {
      citaId: 1, clienteNombre: 'Cliente', clienteTelefono: null, servicioNombre: 'Servicio',
      sucursalNombre: 'Sucursal', inicio: '2026-10-12T09:00:00', total: 200, pagado: 0,
      pendiente: 200, moneda: 'MXN', estadoCita: 'FINALIZADA', estadoCobro: 'PENDIENTE',
      periodoCobro: 'HOY', sucursalId: 3, diasAtraso: 0, precioLista: descuento ? 250 : 200, descuento, promocionTitulo: descuento ? 'Oferta pactada' : null
    };
    Object.assign(fixture.componentInstance, {
      sucursalId: null, citas: [cita], citaSeleccionada: cita, citaSeleccionadaId: 1,
      formularioPago: crearFormularioPago(), metodosPago: [], metodoPagoEfectivo: 'EFECTIVO',
      pagos: [], guardando: false, cajaAbierta: false, mensajeBloqueo: ''
    });
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }
  it('presenta los importes del backend sin restar otra vez el descuento', () => {
    const vista = render(50);
    expect(vista.querySelector('.caja-promotion-breakdown')?.textContent).toContain('Oferta pactada');
    expect(vista.querySelector('del')?.textContent).toContain('$250.00');
    expect(vista.querySelector('.caja-promotion-breakdown')?.textContent).toContain('$50.00');
    expect(vista.querySelector('.caja-payment-highlight')?.textContent).toContain('$200.00');
  });
  it('conserva el contexto de cobro sin bloque de promoción cuando no hay descuento', () => {
    expect(render(0).querySelector('.caja-promotion-breakdown')).toBeNull();
  });
});
