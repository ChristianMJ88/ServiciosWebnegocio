import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeEsMx from '@angular/common/locales/es-MX';
import { of } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';
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
      citaId: 1, clienteId: 42, clienteNombre: 'Cliente', clienteTelefono: null, servicioNombre: 'Servicio',
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
  it('no envía varias citas a Point y muestra su limitación',()=>{
    const crearOrdenPoint=vi.fn();TestBed.configureTestingModule({providers:[{provide:CajaService,useValue:{crearOrdenPoint}}]});const c=TestBed.runInInjectionContext(()=>new CajaPaymentsSectionComponent());const cita={citaId:1,clienteId:42,sucursalId:3,moneda:'MXN',pendiente:200} as CitaPorCobrar;Object.assign(c,{citaSeleccionada:cita,citas:[cita,{...cita,citaId:2}],sucursalId:3,formularioPago:crearFormularioPago(),cajaAbierta:true});c.alternarCita(2);c.cobrarConPoint();expect(crearOrdenPoint).not.toHaveBeenCalled();expect(c.errorPoint()).toContain('una sola cita');
  });
  it('cobra varias citas compatibles por venta y conserva el pago parcial individual',()=>{
    const registrarVenta=vi.fn(()=>of({id:99,folio:'V-test',total:300,moneda:'MXN',detalles:[{id:1,nombre:'Uno',cantidad:1,total:200},{id:2,nombre:'Dos',cantidad:1,total:100}]}));
    TestBed.configureTestingModule({providers:[{provide:CajaService,useValue:{registrarVenta}}]});
    const c=TestBed.runInInjectionContext(()=>new CajaPaymentsSectionComponent());
    const cita={citaId:1,clienteId:42,sucursalId:3,moneda:'MXN',pendiente:200} as CitaPorCobrar;
    Object.assign(c,{citaSeleccionada:cita,citas:[cita,{...cita,citaId:2,pendiente:100},{...cita,citaId:3,clienteId:43}],sucursalId:3,cajaAbierta:true,guardando:false,formularioPago:{...crearFormularioPago(),metodoPago:'TARJETA'}});
    expect(c.otrasCitas.map(i=>i.citaId)).toEqual([2]);c.alternarCita(2);expect(c.cobroMultiple).toBe(true);expect(c.formularioPago.monto).toBe(300);
    c.registrarCobro();expect(registrarVenta).toHaveBeenCalledWith(expect.objectContaining({citaId:null,citaIds:[1,2]}));expect(c.ventaCobrada()?.detalles.length).toBe(2);
    const parcial=vi.fn();c.savePayment.subscribe(parcial);c.registrarCobro();expect(parcial).toHaveBeenCalledOnce();
  });
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
