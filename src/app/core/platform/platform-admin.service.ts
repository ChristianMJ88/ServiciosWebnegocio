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
export interface ProveedorPagoPlataforma { id: number; codigo: string; nombre: string; }
export interface ReglaComisionPlataforma { id: number; proveedorId: number; proveedorNombre: string; empresaId: number | null; empresaNombre: string | null; tipo: 'PORCENTAJE'|'MONTO_FIJO'; valor: number; importeMinimo: number | null; importeMaximo: number | null; moneda: string | null; prioridad: number; vigenteDesde: string; vigenteHasta: string | null; activa: boolean; }
export interface ReglaComisionPayload { proveedorId: number | null; empresaId: number | null; tipo: 'PORCENTAJE'|'MONTO_FIJO'; valor: number; importeMinimo: number | null; importeMaximo: number | null; moneda: string | null; prioridad: number; vigenteDesde: string; vigenteHasta: string | null; activa: boolean; }
export interface SimulacionComision { importeBase: number; comisionFluora: number; netoAntesComisionProveedor: number; moneda: string; }

@Injectable({ providedIn: 'root' })
export class PlatformAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/plataforma`;
  resumen(): Observable<ResumenPlataforma> { return this.http.get<ResumenPlataforma>(`${this.baseUrl}/resumen`); }
  tenants(): Observable<TenantPlataforma[]> { return this.http.get<TenantPlataforma[]>(`${this.baseUrl}/tenants`); }
  proveedoresPago(): Observable<ProveedorPagoPlataforma[]> { return this.http.get<ProveedorPagoPlataforma[]>(`${this.baseUrl}/comisiones/proveedores`); }
  reglasComision(): Observable<ReglaComisionPlataforma[]> { return this.http.get<ReglaComisionPlataforma[]>(`${this.baseUrl}/comisiones/reglas`); }
  crearRegla(payload: ReglaComisionPayload): Observable<ReglaComisionPlataforma> { return this.http.post<ReglaComisionPlataforma>(`${this.baseUrl}/comisiones/reglas`, payload); }
  actualizarRegla(id: number, payload: ReglaComisionPayload): Observable<ReglaComisionPlataforma> { return this.http.put<ReglaComisionPlataforma>(`${this.baseUrl}/comisiones/reglas/${id}`, payload); }
  cambiarEstadoRegla(id: number, activa: boolean): Observable<ReglaComisionPlataforma> { return this.http.patch<ReglaComisionPlataforma>(`${this.baseUrl}/comisiones/reglas/${id}/estado`, { activa }); }
  simularComision(payload: ReglaComisionPayload, importe: number): Observable<SimulacionComision> { return this.http.post<SimulacionComision>(`${this.baseUrl}/comisiones/simular`, { tipo: payload.tipo, valor: payload.valor, importeMinimo: payload.importeMinimo, importeMaximo: payload.importeMaximo, importe, moneda: payload.moneda || 'MXN' }); }
}
