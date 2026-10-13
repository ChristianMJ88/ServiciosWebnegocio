import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, it, expect, vi, afterEach } from 'vitest';
import { AdminPaymentsSectionComponent } from './admin-payments-section.component';
import { PaymentsAdminService, IncidenciaCheckout } from './payments-admin.service';
import { AdminService } from '../../../core/admin/admin.service';

describe('Resolución de incidencias de pagos', () => {
  afterEach(() => TestBed.resetTestingModule());
  const incidencia:IncidenciaCheckout={ordenId:'orden',sucursalId:3,motivo:'Horario ocupado',importe:100,moneda:'MXN',creadaEn:'2026-10-13',citas:[{citaId:10,inicio:'2026-12-01T10:00'}],resoluciones:['REPROGRAMADA','OTRA']};
  function crear(resultado=of(undefined)) {
    const servicio={resolverIncidencia:vi.fn().mockReturnValue(resultado)};
    TestBed.configureTestingModule({providers:[{provide:PaymentsAdminService,useValue:servicio},{provide:AdminService,useValue:{}},{provide:ActivatedRoute,useValue:{}}]});
    const component=TestBed.runInInjectionContext(() => new AdminPaymentsSectionComponent());
    vi.spyOn(component,'cargar').mockImplementation(() => {});
    component.resoluciones['orden']={tipo:'REPROGRAMADA',motivo:'Fecha acordada',citas:incidencia.citas.map(c => ({...c}))};
    return {component,servicio};
  }
  it('envía nuevos horarios sin calcular pagos y actualiza después del éxito', () => {
    const {component,servicio}=crear();component.resolverIncidencia(incidencia);
    expect(servicio.resolverIncidencia).toHaveBeenCalledWith('orden',{tipo:'REPROGRAMADA',motivo:'Fecha acordada',citas:incidencia.citas});
    expect(component.cargar).toHaveBeenCalledOnce();expect(component.procesando()).toBe(false);
  });
  it('mantiene el formulario si backend rechaza el horario', () => {
    const {component}=crear(throwError(() => ({error:{detail:'Horario ocupado'}})));
    component.resolverIncidencia(incidencia);
    expect(component.cargar).not.toHaveBeenCalled();expect(component.error()).toBeTruthy();expect(component.resoluciones['orden'].motivo).toBe('Fecha acordada');
  });
  it('otra resolución no manda fechas de reprogramación', () => {
    const {component,servicio}=crear();component.resoluciones['orden'].tipo='OTRA';component.resolverIncidencia(incidencia);
    expect(servicio.resolverIncidencia).toHaveBeenCalledWith('orden',{tipo:'OTRA',motivo:'Fecha acordada',citas:[]});
  });
});
