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

export interface ConfiguracionCheckout {
  habilitado: boolean;
  proveedor: string;
  conectado: boolean;
  ambiente: string;
  aceptarTarjeta: boolean;
  aceptarTransferencia: boolean;
  aceptarEfectivoReferenciado: boolean;
  permitirPagoProductos: boolean;
  permitirPagoServicios: boolean;
  modalidadServicios: 'PAGO_COMPLETO' | 'ANTICIPO_PORCENTAJE' | 'ANTICIPO_IMPORTE';
  anticipoPorcentaje: number | null;
  anticipoImporte: number | null;
  moneda: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentsAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/pagos/mercado-pago/oauth`;
  private readonly configuracionUrl = `${environment.apiBaseUrl}/admin/pagos/configuracion`;

  estado(): Observable<ConexionProveedorPago> {
    return this.http.get<ConexionProveedorPago>(`${this.baseUrl}/estado`);
  }

  iniciar(): Observable<{ urlAutorizacion: string }> {
    return this.http.get<{ urlAutorizacion: string }>(`${this.baseUrl}/iniciar`);
  }

  conectarPruebas(): Observable<ConexionProveedorPago> {
    return this.http.post<ConexionProveedorPago>(`${this.baseUrl}/prueba`, {});
  }

  desconectar(): Observable<ConexionProveedorPago> {
    return this.http.delete<ConexionProveedorPago>(this.baseUrl);
  }

  obtenerConfiguracion(): Observable<ConfiguracionCheckout> {
    return this.http.get<ConfiguracionCheckout>(this.configuracionUrl);
  }

  guardarConfiguracion(configuracion: ConfiguracionCheckout): Observable<ConfiguracionCheckout> {
    const { proveedor: _proveedor, conectado: _conectado, ambiente: _ambiente, ...request } = configuracion;
    return this.http.put<ConfiguracionCheckout>(this.configuracionUrl, request);
  }
}
