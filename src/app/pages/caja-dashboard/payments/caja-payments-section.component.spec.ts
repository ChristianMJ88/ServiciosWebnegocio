import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeEsMx from '@angular/common/locales/es-MX';
import { of } from 'rxjs';
import { beforeEach, describe, it, expect, vi } from 'vitest';
import { CajaPaymentsSectionComponent } from './caja-payments-section.component';
import { CajaService, CitaPorCobrar } from '../../../core/caja/caja.service';
import { crearFormularioPago } from '../forms/caja.forms';

registerLocaleData(localeEsMx);

describe('Desglose pactado de promociones en Caja', () => {
  function render(descuento: number, conComprobante=false) {
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
    if(conComprobante)fixture.componentInstance.ventaCobrada.set({folio:'V-anterior',total:560,moneda:'MXN',detalles:[{id:1,nombre:'Cita liquidada',cantidad:1,total:560}]} as any);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }
  describe('contrato de clase', () => {
  it('envía varias citas compatibles a Point usando el saldo del backend',()=>{
    const crearOrdenPoint=vi.fn(()=>of({id:'P-test',estado:'CREADO'}));TestBed.configureTestingModule({providers:[{provide:CajaService,useValue:{crearOrdenPoint}}]});
    const c=TestBed.runInInjectionContext(()=>new CajaPaymentsSectionComponent());const cita={citaId:1,clienteId:42,sucursalId:3,moneda:'MXN',pendiente:200} as CitaPorCobrar;
    Object.assign(c,{citaSeleccionada:cita,citas:[cita,{...cita,citaId:2}],sucursalId:3,formularioPago:crearFormularioPago(),cajaAbierta:true,terminalPointId:5});
    (c as any).vigilarPoint=vi.fn();c.alternarCita(2);c.cobrarConPoint({pagos:[{metodoPago:'POINT_TEST',monto:null}],usaPoint:true,terminalId:5,observaciones:''});
    expect(crearOrdenPoint).toHaveBeenCalledWith(5,expect.objectContaining({citaId:null,citaIds:[1,2],pagos:[{metodoPago:'POINT_TEST',monto:null}]}),expect.any(String));expect(c.errorPoint()).toBe('');
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
  });
  describe('DOM con promoción', () => {
  let vista: HTMLElement;
  beforeEach(() => { vista = render(50); });
  it('presenta los importes del backend sin restar otra vez el descuento', () => {
    expect(vista.querySelector('.caja-promotion-breakdown')?.textContent).toContain('Oferta pactada');
    expect(vista.querySelector('del')?.textContent).toContain('$250.00');
    expect(vista.querySelector('.caja-promotion-breakdown')?.textContent).toContain('$50.00');
    expect(vista.querySelector('.caja-payment-highlight')?.textContent).toContain('$200.00');
  });
  });
  describe('DOM sin promoción', () => {
  let vista: HTMLElement;
  beforeEach(() => { vista = render(0); });
  it('conserva el contexto de cobro sin bloque de promoción cuando no hay descuento', () => {
    expect(vista.querySelector('.caja-promotion-breakdown')).toBeNull();
    expect(vista.querySelector('.caja-payment-context')).not.toBeNull();
  });
  });
  describe('DOM con comprobante', () => {
  let vista: HTMLElement;
  beforeEach(() => { vista = render(0,true); });
  it('el comprobante pagado no se mezcla con el contexto o el historial de la siguiente cita',()=>{
    expect(vista.querySelector('app-venta-comprobante')?.textContent).toContain('Cita liquidada');expect(vista.querySelector('app-venta-comprobante')?.textContent).toContain('$560.00');
    expect(vista.querySelector('.caja-payment-context')).toBeNull();expect(vista.querySelector('.caja-history')).toBeNull();
  });
  });
});
