import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ResumenPlataforma {
  tenantsTotal: number;
  tenantsActivos: number;
  usuariosTenant: number;
  cuentasPagoConectadas: number;
  volumenBrutoConciliado: number;
  comisionesProveedor: number;
  comisionesFluora: number;
  netoNegocios: number;
  moneda: string | null;
}

export interface TenantPlataforma {
  id: number;
  nombre: string;
  slug: string;
  estado: string;
  zonaHoraria: string;
  creadoEn: string;
  sucursales: number;
  usuarios: number;
  pagosConectados: boolean;
  volumenBrutoConciliado: number;
  comisionFluora: number;
  netoConciliado: number;
  moneda: string | null;
}
export interface IntegracionPagoTenant { proveedorId: number; proveedorNombre: string; estado: string; ambiente: string; conectadoEn: string | null; ultimaSincronizacionEn: string | null; }
export interface ConfiguracionComercioTenant { checkoutConfigurado: boolean; checkoutHabilitado: boolean; productosHabilitados: boolean; serviciosHabilitados: boolean; moneda: string | null; enviosConfigurados: boolean; enviosHabilitados: boolean; proveedorEnvio: string | null; ambienteEnvio: string | null; }
export interface DetalleTenantPlataforma { tenant: TenantPlataforma; serviciosActivos: number; productosActivos: number; clientes: number; citasUltimos30Dias: number; ventasUltimos30Dias: number; importeVentasUltimos30Dias: number; monedaVentas: string | null; comercio: ConfiguracionComercioTenant; integracionesPago: IntegracionPagoTenant[]; }
export interface ProveedorPagoPlataforma { id: number; codigo: string; nombre: string; }
export interface ReglaComisionPlataforma { id: number; proveedorId: number; proveedorNombre: string; empresaId: number | null; empresaNombre: string | null; tipo: 'PORCENTAJE'|'MONTO_FIJO'; valor: number; importeMinimo: number | null; importeMaximo: number | null; moneda: string | null; prioridad: number; vigenteDesde: string; vigenteHasta: string | null; activa: boolean; }
export interface ReglaComisionPayload { proveedorId: number | null; empresaId: number | null; tipo: 'PORCENTAJE'|'MONTO_FIJO'; valor: number; importeMinimo: number | null; importeMaximo: number | null; moneda: string | null; prioridad: number; vigenteDesde: string; vigenteHasta: string | null; activa: boolean; }
export interface SimulacionComision { importeBase: number; comisionFluora: number; netoAntesComisionProveedor: number; moneda: string; }
export interface ModuloPlataforma { id: number; codigo: string; nombre: string; descripcion: string | null; orden: number; }
export interface ModuloPlan { moduloId: number; codigo: string; nombre: string; limiteUso: number | null; }
export interface PlanPlataforma { id: number; codigo: string; nombre: string; descripcion: string | null; moneda: string; precioMensual: number; precioAnual: number | null; activo: boolean; publico: boolean; modulos: ModuloPlan[]; }
export interface PlanPayload { codigo: string; nombre: string; descripcion: string | null; moneda: string; precioMensual: number; precioAnual: number | null; activo: boolean; publico: boolean; modulos: { moduloId: number; limiteUso: number | null }[]; }
export interface SuscripcionPlataforma { id: number; empresaId: number; empresaNombre: string; planId: number; planNombre: string; estado: string; ciclo: string; precioAcordado: number; moneda: string; iniciaEn: string; terminaEn: string | null; pruebaHasta: string | null; proveedorPagoId: number | null; referenciaExterna: string | null; }
export interface SuscripcionPayload { empresaId: number | null; planId: number | null; ciclo: 'MENSUAL'|'ANUAL'; precioAcordado: number | null; moneda: string | null; estado: 'ACTIVA'|'PRUEBA'; iniciaEn: string; terminaEn: string | null; pruebaHasta: string | null; }

@Injectable({ providedIn: 'root' })
export class PlatformAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/plataforma`;
  resumen(): Observable<ResumenPlataforma> { return this.http.get<ResumenPlataforma>(`${this.baseUrl}/resumen`); }
  tenants(): Observable<TenantPlataforma[]> { return this.http.get<TenantPlataforma[]>(`${this.baseUrl}/tenants`); }
  tenant(id: number): Observable<DetalleTenantPlataforma> { return this.http.get<DetalleTenantPlataforma>(`${this.baseUrl}/tenants/${id}`); }
  proveedoresPago(): Observable<ProveedorPagoPlataforma[]> { return this.http.get<ProveedorPagoPlataforma[]>(`${this.baseUrl}/comisiones/proveedores`); }
  reglasComision(): Observable<ReglaComisionPlataforma[]> { return this.http.get<ReglaComisionPlataforma[]>(`${this.baseUrl}/comisiones/reglas`); }
  crearRegla(payload: ReglaComisionPayload): Observable<ReglaComisionPlataforma> { return this.http.post<ReglaComisionPlataforma>(`${this.baseUrl}/comisiones/reglas`, payload); }
  actualizarRegla(id: number, payload: ReglaComisionPayload): Observable<ReglaComisionPlataforma> { return this.http.put<ReglaComisionPlataforma>(`${this.baseUrl}/comisiones/reglas/${id}`, payload); }
  cambiarEstadoRegla(id: number, activa: boolean): Observable<ReglaComisionPlataforma> { return this.http.patch<ReglaComisionPlataforma>(`${this.baseUrl}/comisiones/reglas/${id}/estado`, { activa }); }
  simularComision(payload: ReglaComisionPayload, importe: number): Observable<SimulacionComision> { return this.http.post<SimulacionComision>(`${this.baseUrl}/comisiones/simular`, { tipo: payload.tipo, valor: payload.valor, importeMinimo: payload.importeMinimo, importeMaximo: payload.importeMaximo, importe, moneda: payload.moneda || 'MXN' }); }
  modulosSuscripcion(): Observable<ModuloPlataforma[]> { return this.http.get<ModuloPlataforma[]>(`${this.baseUrl}/suscripciones/modulos`); }
  planes(): Observable<PlanPlataforma[]> { return this.http.get<PlanPlataforma[]>(`${this.baseUrl}/suscripciones/planes`); }
  crearPlan(payload: PlanPayload): Observable<PlanPlataforma> { return this.http.post<PlanPlataforma>(`${this.baseUrl}/suscripciones/planes`,payload); }
  actualizarPlan(id: number,payload: PlanPayload): Observable<PlanPlataforma> { return this.http.put<PlanPlataforma>(`${this.baseUrl}/suscripciones/planes/${id}`,payload); }
  suscripciones(): Observable<SuscripcionPlataforma[]> { return this.http.get<SuscripcionPlataforma[]>(`${this.baseUrl}/suscripciones`); }
  asignarSuscripcion(payload: SuscripcionPayload): Observable<SuscripcionPlataforma> { return this.http.post<SuscripcionPlataforma>(`${this.baseUrl}/suscripciones`,payload); }
}
