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

@Injectable({ providedIn: 'root' })
export class PlatformAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/plataforma`;
  resumen(): Observable<ResumenPlataforma> { return this.http.get<ResumenPlataforma>(`${this.baseUrl}/resumen`); }
  tenants(): Observable<TenantPlataforma[]> { return this.http.get<TenantPlataforma[]>(`${this.baseUrl}/tenants`); }
}
