import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { signal } from '@angular/core';
import { CheckoutComponent } from './checkout.component';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { ProductCartService } from '../../services/product-cart.service';
import { PublicCheckoutService, CheckoutPayment } from '../../services/public-checkout.service';
import { BookingDataService } from '../../services/booking-data.service';
import { MercadoPagoBricksService } from '../../services/mercado-pago-bricks.service';
import { AddressValidationService } from '../../services/address-validation.service';
import { AppointmentCheckoutDraftService } from '../../services/appointment-checkout-draft.service';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

const pago = (estadoOrden: string): CheckoutPayment => ({ordenId:'orden',estadoOrden,estadoPago:'RECHAZADO',proveedor:'MERCADO_PAGO',referenciaExterna:null,referenciaPagoExterna:null,importe:100,moneda:'MXN',accionUrl:null,codigoRespuesta:null,detalleRespuesta:null});
function crear(checkout: any = {}) {
  TestBed.configureTestingModule({providers:[
    {provide:TenantContextService,useValue:{slug:()=> 'negocio'}},
    {provide:ProductCartService,useValue:{items:signal([]),clear:vi.fn()}},
    {provide:PublicCheckoutService,useValue:checkout},
    {provide:BookingDataService,useValue:{}},
    {provide:MercadoPagoBricksService,useValue:{}},
    {provide:ActivatedRoute,useValue:{snapshot:{queryParamMap:{get:()=>null}}}},
    {provide:AddressValidationService,useValue:{}},
    {provide:AppointmentCheckoutDraftService,useValue:{}}
  ]});
  return TestBed.runInInjectionContext(() => new CheckoutComponent());
}
describe('Checkout: decisiones de reserva del backend', () => {
  it('explica que se debe recargar cuando el 409 cambia la política y falla la configuración', async()=>{
    const c=crear({config:()=>throwError(()=>new Error('sin conexión'))});
    c.config.set({habilitado:true} as any); await (c as any).resetPolicyOrder('negocio');
    expect(c.config()).toBeNull();expect(c.error()).toContain('Recarga la página');expect(c.error()).toContain('política cambió');
  });
  it('un pago con incidencia no anuncia una cita confirmada', () => {
    const componente=crear();componente.appointmentIds.set([8]);
    componente.payment.set({...pago('PAGADA_CON_INCIDENCIA'),estadoPago:'APROBADO'});
    expect(componente.postPaymentMessage()).toContain('requiere revisión');
    expect(componente.postPaymentMessage()).toContain('aún no está confirmada');
  });
  it('solo reabre el formulario si backend permite reintentar sobre la orden pendiente', async () => {
    const componente=crear();componente.payment.set(pago('EXPIRADA'));
    await componente.retryPayment();expect(componente.payment()?.estadoOrden).toBe('EXPIRADA');
    componente.payment.set(pago('PENDIENTE_PAGO'));
    const claveAnterior=(componente as any).paymentKey;
    await componente.retryPayment();
    expect(componente.payment()).toBeNull();
    expect((componente as any).paymentKey).not.toBe(claveAnterior);
  });
  it('no crea una orden de servicios sin aceptación explícita', async () => {
    const createOrder = vi.fn(); const componente = crear({createOrder});
    componente.config.set({habilitado:true, politica:{version:5,vigenteDesde:'2026-10-17T16:00:00Z'}} as any);
    componente.appointmentIds.set([8]); componente.form.patchValue({email:'c@example.com',name:'Cliente',phone:'2220000000',branchId:2});
    await componente.confirmOrder(); expect(createOrder).not.toHaveBeenCalled(); expect(componente.error()).toContain('Acepta');
  });
  it('productos solos envían aceptación nula y servicios la versión recibida del backend', async () => {
    const createOrder = vi.fn().mockReturnValue(of({id:'orden',estado:'PENDIENTE_PAGO',detalles:[]}));
    const componente = crear({createOrder}); componente.config.set({habilitado:true, politica:{version:5,vigenteDesde:'2026-10-17T16:00:00Z'}} as any);
    componente.form.patchValue({email:'c@example.com',name:'Cliente',phone:'2220000000',branchId:2});
    componente.selectedPaymentMethod.set('');
    (componente.cart.items as any).set([{productoId:44,cantidad:1}]);
    await componente.confirmOrder(); expect(createOrder.mock.calls[0][1].politicaVersion).toBeNull(); expect(createOrder.mock.calls[0][1].aceptaPolitica).toBeNull();
    componente.appointmentIds.set([8]); componente.form.controls.acceptPolicy.setValue(true);
    await componente.confirmOrder(); expect(createOrder.mock.calls[1][1].politicaVersion).toBe(5); expect(createOrder.mock.calls[1][1].aceptaPolitica).toBe(true);
  });

  it('409 al pagar permite volver a aceptar y crear otra orden con claves nuevas', async () => {
    const checkout = {payCard:vi.fn().mockReturnValue(throwError(()=>({status:409,error:{message:'La política cambió'}}))),
      config:vi.fn().mockReturnValue(of({habilitado:true,politica:{version:6,vigenteDesde:'2026-10-17T17:00:00Z'}}))};
    const c=crear(checkout); c.appointmentIds.set([8]); c.form.controls.acceptPolicy.setValue(true);
    c.order.set({id:'orden-anterior'} as any); const key=(c as any).orderKey; const paymentKey=(c as any).paymentKey;
    await expect((c as any).pay({})).rejects.toMatchObject({status:409});
    expect(c.order()).toBeNull(); expect(c.form.controls.acceptPolicy.value).toBe(false);
    expect((c as any).paymentKey).not.toBe(paymentKey); expect((c as any).orderKey).not.toBe(key); expect(c.config()?.politica?.version).toBe(6);
  });

});
