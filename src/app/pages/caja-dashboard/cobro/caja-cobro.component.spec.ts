import {TestBed} from '@angular/core/testing';
import {CajaCobroComponent} from './caja-cobro.component';
import {OpcionCaja,OrdenPointCaja} from '../../../core/caja/caja.service';
import {vi} from 'vitest';
const metodos:OpcionCaja[]=[{codigo:'CASH_TEST',etiqueta:'Efectivo',esEfectivo:true,denominaciones:{MXN:[200,500,1000]}},{codigo:'CARD_TEST',etiqueta:'Tarjeta manual',requiereReferencia:true,ayuda:'Captura el comprobante de la terminal externa.'}];
function render(terminal=false){
 TestBed.configureTestingModule({imports:[CajaCobroComponent]});const f=TestBed.createComponent(CajaCobroComponent);
 f.componentRef.setInput('total',700);f.componentRef.setInput('moneda','MXN');f.componentRef.setInput('metodos',metodos);
 if(terminal)f.componentRef.setInput('terminales',[{id:5,nombre:'Mi terminal',activa:true,metodoPago:{codigo:'POINT_TEST',etiqueta:'Tarjeta (Point)',esPoint:true}}]);
 f.changeDetectorRef.markForCheck();f.detectChanges();return f;
}
describe('Panel único de cobro',()=>{
 it('muestra solo el detalle efectivo y métodos ofrecidos por backend',()=>{
  const f=render(true),c=f.componentInstance;expect(c.lineas[0].metodoPago).toBe('CASH_TEST');
  expect(f.nativeElement.querySelector('select')).toBeNull();expect(f.nativeElement.textContent).toContain('Efectivo recibido');
  expect(f.nativeElement.textContent).not.toContain('El importe aparecerá');expect(c.atajos(c.lineas[0])).toEqual([800,1000]);
  expect(c.cta).toContain('$700.00');expect(c.motivo).toBe('');
 });
 it('Point solo se ofrece con terminal activa y toma código/etiqueta del backend',()=>{
  const f=render(),c=f.componentInstance;expect(c.disponibles.some(m=>m.esPoint)).toBe(false);
  f.componentRef.setInput('terminales',[{id:8,activa:true,nombre:'Nueva',metodoPago:{codigo:'CUSTOM',etiqueta:'Dispositivo configurado',esPoint:true}}]);f.changeDetectorRef.markForCheck();f.detectChanges();
  c.elegir(c.lineas[0],'CUSTOM');f.changeDetectorRef.markForCheck();f.detectChanges();expect(c.cta).toContain('Enviar');expect(c.terminalId).toBe(8);
  expect(f.nativeElement.querySelector('input[readonly]')).not.toBeNull();expect(f.nativeElement.textContent).not.toContain('Efectivo recibido');
 });
 it('divide efectivo/Point, el resto es readonly y cambio solo del efectivo',()=>{
  const f=render(true),c=f.componentInstance;c.dividir();c.cambiarMonto(c.lineas[0],300);c.lineas[0].recibido=500;c.elegir(c.lineas[1],'POINT_TEST');f.changeDetectorRef.markForCheck();f.detectChanges();
  expect(c.importe(c.lineas[1])).toBe(400);expect(c.cambio(c.lineas[0])).toBe(200);expect(c.cambio(c.lineas[1])).toBe(0);expect(c.cta).toContain('$300.00');expect(c.cta).toContain('$400.00');
  const cobrar=vi.fn();c.cobrar.subscribe(cobrar);c.enviar();expect(cobrar).toHaveBeenCalledWith(expect.objectContaining({usaPoint:true,pagos:[expect.objectContaining({metodoPago:'CASH_TEST',monto:300,montoRecibido:500}),expect.objectContaining({metodoPago:'POINT_TEST',monto:null})]}));
  expect(f.nativeElement.querySelector('input[aria-readonly="true"]')).not.toBeNull();
  c.quitar(0);expect(c.importe(c.lineas[0])).toBe(700);expect(c.lineas).toHaveLength(1);
 });
 it('expone referencia y exige la condición que devuelve backend',()=>{
  const f=render(),c=f.componentInstance;c.elegir(c.lineas[0],'CARD_TEST');f.changeDetectorRef.markForCheck();f.detectChanges();expect(c.motivo).toContain('referencia');
  expect(f.nativeElement.querySelector('input[maxlength="120"]')).not.toBeNull();c.lineas[0].referencia='EXT-123';expect(c.motivo).toBe('');
 });
 it('espera sin formulario y conserva el reparto al rechazar',()=>{
  const f=render(true),c=f.componentInstance;c.dividir();c.cambiarMonto(c.lineas[0],300);c.lineas[0].recibido=500;c.elegir(c.lineas[1],'POINT_TEST');
  f.componentRef.setInput('orden',{id:'P',estado:'CREADO',importe:400,terminalNombre:'Mi terminal',proveedorOrdenId:'ORD'} as OrdenPointCaja);f.changeDetectorRef.markForCheck();f.detectChanges();expect(f.nativeElement.querySelector('form')).toBeNull();expect(f.nativeElement.textContent).toContain('Esperando la terminal');
  f.componentRef.setInput('orden',{estado:'RECHAZADO'});f.componentRef.setInput('error','No se registró el efectivo de $300: devuélvelo.');f.changeDetectorRef.markForCheck();f.detectChanges();expect(f.nativeElement.querySelector('form')).not.toBeNull();expect(c.lineas[0].monto).toBe(300);expect(c.lineas[0].recibido).toBe(500);expect(f.nativeElement.textContent).toContain('No se registró el efectivo');
 });
 it('no permite monto inválido ni efectivo insuficiente y conserva abonos individuales',()=>{
  const f=render(),c=f.componentInstance;c.lineas[0].recibido=600;expect(c.motivo).toContain('no alcanza');c.lineas[0].recibido=700;c.lineas[0].monto=300;expect(c.motivo).toContain('coincidir');
  f.componentRef.setInput('permiteAbono',true);f.changeDetectorRef.markForCheck();f.detectChanges();expect(c.motivo).toBe('');c.lineas[0].monto=-1;expect(c.motivo).toContain('mayor que cero');
 });
 it('solo ofrece resolver y reintentar cuando el backend autoriza cada acción',()=>{
  const f=render(true);
  f.componentRef.setInput('orden',{id:'P',estado:'REQUIERE_REVISION',importe:400,terminalNombre:'Terminal',puedeResolver:false,puedeReintentar:false});f.changeDetectorRef.markForCheck();f.detectChanges();
  expect(f.nativeElement.textContent).toContain('Revisar cobro');expect(f.nativeElement.textContent).not.toContain('Guardar resolución');expect(f.nativeElement.textContent).not.toContain('Reintentar el mismo');
  f.componentRef.setInput('orden',{id:'P',estado:'REQUIERE_REVISION',importe:400,terminalNombre:'Terminal',puedeResolver:true});f.changeDetectorRef.markForCheck();f.detectChanges();
  expect(f.nativeElement.textContent).toContain('Guardar resolución verificada');expect(f.nativeElement.querySelector('form')).toBeNull();
  f.componentRef.setInput('orden',{id:'P',estado:'CREANDO',importe:400,terminalNombre:'Terminal',puedeReintentar:true});f.changeDetectorRef.markForCheck();f.detectChanges();
  expect(f.nativeElement.textContent).toContain('Reintentar el mismo envío');expect(f.nativeElement.textContent).not.toContain('Guardar resolución');
 });
 it('muestra todos los métodos y el cambio del comprobante definitivo recibido',()=>{
  const f=render();
  f.componentRef.setInput('comprobante',{folio:'V-TEST',total:700,moneda:'MXN',metodoPago:'MIXTO',detalles:[],pagos:[{etiqueta:'Efectivo',monto:300,montoRecibido:500,cambio:200},{etiqueta:'Tarjeta (Point)',monto:400,referencia:'PAGO-TEST'}]});f.changeDetectorRef.markForCheck();f.detectChanges();
  const receipt=f.nativeElement.querySelector('app-venta-comprobante').textContent;
  expect(receipt).toContain('$700.00');expect(receipt).toContain('$300.00');expect(receipt).toContain('$400.00');expect(receipt).toContain('$200.00');expect(receipt).toContain('PAGO-TEST');expect(f.nativeElement.querySelector('form')).toBeNull();
 });
});
