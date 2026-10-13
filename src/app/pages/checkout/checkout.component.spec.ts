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
import { vi } from 'vitest';

const pago = (estadoOrden: string): CheckoutPayment => ({ordenId:'orden',estadoOrden,estadoPago:'RECHAZADO',proveedor:'MERCADO_PAGO',referenciaExterna:null,referenciaPagoExterna:null,importe:100,moneda:'MXN',accionUrl:null,codigoRespuesta:null,detalleRespuesta:null});
function crear() {
  TestBed.configureTestingModule({providers:[
    {provide:TenantContextService,useValue:{}},
    {provide:ProductCartService,useValue:{items:signal([]),clear:vi.fn()}},
    {provide:PublicCheckoutService,useValue:{}},
    {provide:BookingDataService,useValue:{}},
    {provide:MercadoPagoBricksService,useValue:{}},
    {provide:ActivatedRoute,useValue:{}},
    {provide:AddressValidationService,useValue:{}},
    {provide:AppointmentCheckoutDraftService,useValue:{}}
  ]});
  return TestBed.runInInjectionContext(() => new CheckoutComponent());
}
describe('Checkout: decisiones de reserva del backend', () => {
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
});
