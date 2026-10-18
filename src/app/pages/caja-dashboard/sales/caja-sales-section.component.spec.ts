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
  (c as any).cargar=vi.fn();c.confirmarPanel({pagos:[{metodoPago:"TARJETA",monto:300}],usaPoint:false,terminalId:null,observaciones:""});expect(registrarVenta).toHaveBeenCalledWith(expect.objectContaining({citaId:null,citaIds:[1,2]}));expect(registrarVenta.mock.calls[0][0].total).toBeUndefined();
 });
 it('el comprobante presenta una línea por servicio del backend',()=>{
  TestBed.configureTestingModule({imports:[VentaComprobanteComponent]});const f=TestBed.createComponent(VentaComprobanteComponent);f.componentRef.setInput('venta',{folio:'V-test',moneda:'MXN',total:300,detalles:[{id:1,nombre:'Manos',cantidad:1,total:100},{id:2,nombre:'Cejas',cantidad:1,total:200}]});f.detectChanges();expect(f.nativeElement.textContent).toContain('Manos');expect(f.nativeElement.textContent).toContain('Cejas');expect(f.nativeElement.textContent).toContain('$300.00');
 });
 it('mantiene localizable una orden en revisión aunque existan seis finales más recientes',()=>{
  TestBed.configureTestingModule({imports:[CajaSalesSectionComponent],providers:[{provide:CajaService,useValue:{}}]});
  const f=TestBed.createComponent(CajaSalesSectionComponent);Object.assign(f.componentInstance,{citas:[],metodosPago:[],sucursalId:null,cajaAbierta:true});
  f.componentInstance.vistaPos.set('actividad');f.componentInstance.historialPoint.set([...Array.from({length:6},(_,i)=>({id:'F'+i,folioVenta:'Final '+i,estado:'CANCELADO',importe:100,moneda:'MXN'})),{id:'P',folioVenta:'Pendiente visible',estado:'REQUIERE_REVISION',importe:400,moneda:'MXN'}] as any);
  f.detectChanges();expect(f.nativeElement.textContent).toContain('Pendiente visible');expect(f.nativeElement.textContent).toContain('Continuar cobro');
 });
 it('retoma una orden abriendo el panel también en móvil y permite ver aprobaciones con incidencia sin reenviar',()=>{
  TestBed.configureTestingModule({providers:[{provide:CajaService,useValue:{}}]});const c=TestBed.runInInjectionContext(()=>new CajaSalesSectionComponent());
  const vigilar=vi.fn();(c as any).vigilarPoint=vigilar;c.vistaPos.set('actividad');c.continuarOrdenPoint({id:'P',estado:'REQUIERE_REVISION'} as any);
  expect(c.vistaPos()).toBe('catalogo');expect(c.cuentaMovilAbierta()).toBe(true);expect(vigilar).toHaveBeenCalledWith('P');
  c.guardando.set(false);vigilar.mockClear();c.vistaPos.set('actividad');c.continuarOrdenPoint({id:'AP',estado:'APROBADO',requiereRevision:true} as any);
  expect(c.ordenPoint()?.id).toBe('AP');expect(c.cuentaMovilAbierta()).toBe(true);expect(c.guardando()).toBe(false);expect(vigilar).not.toHaveBeenCalled();
  const actualizar=vi.fn(),cobrado=vi.fn();c.pointStateChanged.subscribe(actualizar);c.saleCompleted.subscribe(cobrado);(c as any).finalizarPoint({id:'P',estado:'CANCELADO',moneda:'MXN',efectivoPrevisto:300});
  expect(actualizar).toHaveBeenCalledOnce();expect(cobrado).not.toHaveBeenCalled();expect(c.error()).toContain('No se registró el efectivo');
 });
});
