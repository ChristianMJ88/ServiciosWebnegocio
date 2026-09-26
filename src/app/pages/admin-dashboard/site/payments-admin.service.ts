import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface ConexionProveedorPago {
  proveedor: string;
  estado: 'NO_CONECTADO' | 'CONECTADO' | 'DESCONECTADO' | string;
  ambiente: string;
  cuentaExternaId: string | null;
  correoCuenta: string | null;
  conectadoEn: string | null;
  tokenExpiraEn: string | null;
}

@Injectable({ providedIn: 'root' })
export class PaymentsAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/pagos/mercado-pago/oauth`;

  estado(): Observable<ConexionProveedorPago> {
    return this.http.get<ConexionProveedorPago>(`${this.baseUrl}/estado`);
  }

  iniciar(): Observable<{ urlAutorizacion: string }> {
    return this.http.get<{ urlAutorizacion: string }>(`${this.baseUrl}/iniciar`);
  }

  desconectar(): Observable<ConexionProveedorPago> {
    return this.http.delete<ConexionProveedorPago>(this.baseUrl);
  }
}
