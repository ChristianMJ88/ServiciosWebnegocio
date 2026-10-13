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
  mostrarEstadoPago: boolean;
  permitirPagoProductos: boolean;
  permitirPagoServicios: boolean;
  permitirRecoleccion: boolean;
  permitirEntregaLocal: boolean;
  costoEntregaLocal: number;
  permitirEnvioNacional: boolean;
  costoEnvioNacional: number;
  modalidadServicios: 'PAGO_COMPLETO' | 'ANTICIPO_PORCENTAJE' | 'ANTICIPO_IMPORTE';
  anticipoPorcentaje: number | null;
  anticipoImporte: number | null;
  moneda: string;
}

export interface TerminalPoint {
  id: number | null;
  terminalId: string;
  posId: string | null;
  storeId: string | null;
  externalPosId: string | null;
  nombre: string;
  modoOperacion: string | null;
  sucursalId: number | null;
  sucursalNombre: string | null;
  activa: boolean;
  configurada: boolean;
}

export interface WebhookMercadoPago { url: string | null; disponible: boolean; }

export interface MetodoPagoCheckout {
  id: string;
  nombre: string;
  tipo: string;
  estadoProveedor: string;
  imagenUrl: string | null;
  activo: boolean;
  soloProductos: boolean;
  vigenciaDias: number;
}

export interface ConciliacionPago {
  id: number;
  canal: 'CHECKOUT' | 'POINT' | string;
  estado: 'PENDIENTE_CONCILIACION' | 'CONCILIADA' | 'REEMBOLSADA' | string;
  proveedor: string;
  referenciaOrden: string;
  referenciaPago: string | null;
  moneda: string;
  importeBruto: number;
  comisionProveedorTotal: number | null;
  impuestoComisionProveedor: number | null;
  comisionPlataforma: number | null;
  impuestoComisionPlataforma: number | null;
  retenciones: number | null;
  importeReembolsado: number;
  importeNeto: number | null;
  liquidacionDefinitiva: boolean;
  liquidadoEn: string | null;
  actualizadoEn: string;
}

export interface ConfiguracionReporteLiberacion {
  prefijo: string | null;
  zonaHoraria: string | null;
  programado: boolean;
  idioma: string | null;
  columnas: string[];
  columnasFaltantes: string[];
  listaParaConciliar: boolean;
  frecuenciaTipo: string | null;
  frecuenciaHora: number | null;
  frecuenciaValor: string | null;
  incluirRetirosAlFinal: boolean;
  ejecutarDespuesDeRetiro: boolean;
}

export interface GuardarConfiguracionReporteLiberacion {
  prefijo: string;
  zonaHoraria: string;
  frecuenciaTipo: 'daily' | 'weekly' | 'monthly';
  frecuenciaHora: number;
  frecuenciaValor: string;
  incluirRetirosAlFinal: boolean;
  ejecutarDespuesDeRetiro: boolean;
}

export interface ReporteLiberacion {
  id: number;
  estado: string;
  tareaExternaId: string;
  reporteExternoId: string | null;
  nombreArchivo: string | null;
  fechaDesde: string;
  fechaHasta: string;
  filasProcesadas: number;
  errorUltimo: string | null;
  solicitadoEn: string;
  procesadoEn: string | null;
}

export interface IncidenciaCheckout { ordenId:string; sucursalId:number; motivo:string; importe:number; moneda:string; creadaEn:string; citas:{citaId:number; inicio:string}[]; resoluciones:string[]; }

@Injectable({ providedIn: 'root' })
export class PaymentsAdminService {
  resolverIncidencia(ordenId:string, request:{tipo:string; motivo:string; citas:{citaId:number; inicio:string}[]}): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/admin/pagos/conciliaciones/incidencias/${encodeURIComponent(ordenId)}/resolver`,request);
  }
  incidencias(): Observable<IncidenciaCheckout[]> { return this.http.get<IncidenciaCheckout[]>(`${environment.apiBaseUrl}/admin/pagos/conciliaciones/incidencias`); }

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/pagos/mercado-pago/oauth`;
  private readonly configuracionUrl = `${environment.apiBaseUrl}/admin/pagos/configuracion`;
  private readonly pointUrl = `${environment.apiBaseUrl}/admin/pagos/point/terminales`;
  private readonly metodosUrl = `${environment.apiBaseUrl}/admin/pagos/metodos`;
  private readonly conciliacionesUrl = `${environment.apiBaseUrl}/admin/pagos/conciliaciones`;
  private readonly reportesUrl = `${environment.apiBaseUrl}/admin/pagos/reportes-liberacion`;

  estado(): Observable<ConexionProveedorPago> {
    return this.http.get<ConexionProveedorPago>(`${this.baseUrl}/estado`);
  }

  webhook(): Observable<WebhookMercadoPago> {
    return this.http.get<WebhookMercadoPago>(`${this.baseUrl}/webhook`);
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

  terminalesPoint(): Observable<TerminalPoint[]> {
    return this.http.get<TerminalPoint[]>(this.pointUrl);
  }

  guardarTerminalPoint(payload: { terminalId: string; sucursalId: number; nombre: string; activa: boolean }): Observable<TerminalPoint> {
    return this.http.put<TerminalPoint>(this.pointUrl, payload);
  }

  metodosPago(): Observable<MetodoPagoCheckout[]> {
    return this.http.get<MetodoPagoCheckout[]>(this.metodosUrl);
  }

  guardarMetodoPago(payload: { metodoId: string; activo: boolean; soloProductos: boolean; vigenciaDias: number }): Observable<MetodoPagoCheckout> {
    return this.http.put<MetodoPagoCheckout>(this.metodosUrl, payload);
  }

  conciliaciones(): Observable<ConciliacionPago[]> {
    return this.http.get<ConciliacionPago[]>(this.conciliacionesUrl);
  }

  configuracionReporteLiberacion(): Observable<ConfiguracionReporteLiberacion> {
    return this.http.get<ConfiguracionReporteLiberacion>(`${this.reportesUrl}/configuracion`);
  }

  guardarConfiguracionReporte(payload: GuardarConfiguracionReporteLiberacion, crear = false): Observable<ConfiguracionReporteLiberacion> {
    return crear
      ? this.http.post<ConfiguracionReporteLiberacion>(`${this.reportesUrl}/configuracion`, payload)
      : this.http.put<ConfiguracionReporteLiberacion>(`${this.reportesUrl}/configuracion`, payload);
  }

  cambiarProgramacionReporte(activar: boolean): Observable<ConfiguracionReporteLiberacion> {
    return activar
      ? this.http.post<ConfiguracionReporteLiberacion>(`${this.reportesUrl}/programacion`, {})
      : this.http.delete<ConfiguracionReporteLiberacion>(`${this.reportesUrl}/programacion`);
  }

  reportesLiberacion(): Observable<ReporteLiberacion[]> {
    return this.http.get<ReporteLiberacion[]>(this.reportesUrl);
  }

  solicitarReporteLiberacion(desde: string, hasta: string): Observable<ReporteLiberacion> {
    return this.http.post<ReporteLiberacion>(this.reportesUrl, { desde, hasta });
  }

  sincronizarReporteLiberacion(id: number): Observable<ReporteLiberacion> {
    return this.http.post<ReporteLiberacion>(`${this.reportesUrl}/${id}/sincronizar`, {});
  }
}
