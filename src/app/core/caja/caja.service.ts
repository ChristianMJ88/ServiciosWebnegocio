import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SucursalCaja {
  id: number;
  empresaId: number;
  nombre: string;
  direccion: string | null;
  telefono: string | null;
  zonaHoraria: string;
}

export interface CajaSesion {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  estado: string;
  montoInicial: number;
  montoEsperado: number;
  montoContado: number | null;
  diferencia: number | null;
  observaciones: string | null;
  abiertaPorUsuarioId: number;
  abiertaEn: string;
  cerradaPorUsuarioId: number | null;
  cerradaEn: string | null;
}

export interface CitaPorCobrar {
  citaId: number;
  clienteNombre: string;
  clienteTelefono: string | null;
  servicioNombre: string;
  sucursalNombre: string;
  inicio: string;
  total: number;
  pagado: number;
  pendiente: number;
  moneda: string;
  estadoCita: string;
  estadoPago: string;
}

export interface PagoCita {
  id: number;
  citaId: number;
  cajaSesionId: number;
  monto: number;
  metodoPago: string;
  referencia: string | null;
  observaciones: string | null;
  registradoPorUsuarioId: number;
  registradoEn: string;
}

export interface MovimientoCaja {
  id: number;
  cajaSesionId: number;
  citaId: number | null;
  tipoMovimiento: string;
  metodoPago: string | null;
  monto: number;
  concepto: string;
  referencia: string | null;
  observaciones: string | null;
  registradoPorUsuarioId: number;
  registradoEn: string;
}

export interface ResumenCaja {
  sesion: CajaSesion | null;
  totalCobrado: number;
  totalCobradoEfectivo: number;
  totalCobradoTarjeta: number;
  totalCobradoTransferencia: number;
  totalGastos: number;
  totalRetiros: number;
  saldoEsperadoCaja: number;
  citasPendientesDeCobro: number;
}

export interface AbrirCajaPayload {
  sucursalId: number;
  montoInicial: number;
  observaciones: string | null;
}

export interface CerrarCajaPayload {
  montoContado: number;
  observaciones: string | null;
}

export interface RegistrarPagoPayload {
  monto: number;
  metodoPago: string;
  referencia: string | null;
  observaciones: string | null;
}

export interface RegistrarMovimientoCajaPayload {
  sucursalId: number;
  tipoMovimiento: string;
  monto: number;
  metodoPago: string | null;
  concepto: string;
  referencia: string | null;
  observaciones: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class CajaService {
  private readonly http = inject(HttpClient);

  getSucursales(): Observable<SucursalCaja[]> {
    const params = new HttpParams().set('empresaId', environment.empresaId);
    return this.http.get<SucursalCaja[]>(`${environment.apiBaseUrl}/publico/sucursales`, { params });
  }

  getSesionActual(sucursalId?: number | null): Observable<CajaSesion | null> {
    let params = new HttpParams();
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<CajaSesion | null>(`${environment.apiBaseUrl}/caja/sesiones/actual`, { params });
  }

  abrirCaja(payload: AbrirCajaPayload): Observable<CajaSesion> {
    return this.http.post<CajaSesion>(`${environment.apiBaseUrl}/caja/sesiones/abrir`, payload);
  }

  cerrarCaja(id: number, payload: CerrarCajaPayload): Observable<CajaSesion> {
    return this.http.post<CajaSesion>(`${environment.apiBaseUrl}/caja/sesiones/${id}/cerrar`, payload);
  }

  getCitasPorCobrar(sucursalId?: number | null): Observable<CitaPorCobrar[]> {
    let params = new HttpParams();
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<CitaPorCobrar[]>(`${environment.apiBaseUrl}/caja/citas-por-cobrar`, { params });
  }

  registrarPago(citaId: number, payload: RegistrarPagoPayload): Observable<PagoCita> {
    return this.http.post<PagoCita>(`${environment.apiBaseUrl}/caja/citas/${citaId}/pagos`, payload);
  }

  listarPagosCita(citaId: number): Observable<PagoCita[]> {
    return this.http.get<PagoCita[]>(`${environment.apiBaseUrl}/caja/citas/${citaId}/pagos`);
  }

  registrarMovimiento(payload: RegistrarMovimientoCajaPayload): Observable<MovimientoCaja> {
    return this.http.post<MovimientoCaja>(`${environment.apiBaseUrl}/caja/movimientos`, payload);
  }

  getResumen(sucursalId?: number | null): Observable<ResumenCaja> {
    let params = new HttpParams();
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<ResumenCaja>(`${environment.apiBaseUrl}/caja/resumen`, { params });
  }
}
