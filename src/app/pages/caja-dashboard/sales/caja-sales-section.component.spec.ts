import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {vi} from 'vitest';
import {CajaSalesSectionComponent} from './caja-sales-section.component';
import {CajaService,CitaPorCobrar} from '../../../core/caja/caja.service';
import {VentaComprobanteComponent} from '../../../shared/components/venta-comprobante.component';
describe('Punto de venta con varias citas',()=>{
 it('selecciona solo citas compatibles y envía sus ids sin inventar importes',()=>{
  const registrarVenta=vi.fn((_payload:any)=>of({folio:'V-1',detalles:[]}));TestBed.configureTestingModule({providers:[{provide:CajaService,useValue:{registrarVenta}}]});
  const c=TestBed.runInInjectionContext(()=>new CajaSalesSectionComponent());const primera={citaId:1,clienteId:5,sucursalId:3,moneda:'MXN',pendiente:200} as CitaPorCobrar;
  Object.assign(c,{citas:[primera,{...primera,citaId:2,pendiente:100},{...primera,citaId:3,clienteId:6}],sucursalId:3,cajaAbierta:true,metodoPago:'TARJETA'});
  c.seleccionarCita(1);c.seleccionarCita(2);c.seleccionarCita(3);expect(c.citaIds()).toEqual([1,2]);expect(c.totalEstimado()).toBe(300);expect(c.compatible(c.citas[2])).toBe(false);
  (c as any).cargar=vi.fn();c.cobrar();expect(registrarVenta).toHaveBeenCalledWith(expect.objectContaining({citaId:null,citaIds:[1,2]}));expect(registrarVenta.mock.calls[0][0].total).toBeUndefined();
 });
 it('el comprobante presenta una línea por servicio del backend',()=>{
  TestBed.configureTestingModule({imports:[VentaComprobanteComponent]});const f=TestBed.createComponent(VentaComprobanteComponent);f.componentRef.setInput('venta',{folio:'V-test',moneda:'MXN',total:300,detalles:[{id:1,nombre:'Manos',cantidad:1,total:100},{id:2,nombre:'Cejas',cantidad:1,total:200}]});f.detectChanges();expect(f.nativeElement.textContent).toContain('Manos');expect(f.nativeElement.textContent).toContain('Cejas');expect(f.nativeElement.textContent).toContain('$300.00');
 });
});
