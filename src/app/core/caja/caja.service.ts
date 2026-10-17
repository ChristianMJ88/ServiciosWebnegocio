import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SucursalCaja {
  id: number;
  empresaId: number;
  nombre: string;
  direccion: string | null;
  telefono: string | null;
  zonaHoraria: string;
}

export interface CatalogoCaja {
  sucursalActivaId: number | null;
  sucursales: SucursalCaja[];
  metodosPago: OpcionCaja[];
  tiposMovimiento: OpcionCaja[];
  estadosSesion: OpcionCaja[];
  estadoSesionAbierta: string;
  estadoSesionCerrada: string;
  metodoPagoEfectivo: string;
}

export interface OpcionCaja {
  codigo: string;
  etiqueta: string;
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
  abiertaEn: string;
  cerradaEn: string | null;
}

export interface CitaPorCobrar {
  clienteId: number;
  folio?: string | null;
  sucursalId?: number;
  periodoCobro?: string;
  diasAtraso?: number;
  precioLista?: number;
  descuento?: number;
  promocionTitulo?: string | null;
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
  estadoCobro: string;
  /** Alias legado; las pantallas leen estadoCobro. */
  estadoPago?: string;
}

export interface PagoCita {
  id: number;
  citaId: number;
  cajaSesionId: number;
  monto: number;
  metodoPago: string;
  referencia: string | null;
  observaciones: string | null;
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
  registradoEn: string;
}

export interface ResumenCaja {
  sesion: CajaSesion | null;
  totalCobrado: number;
  totalCobradoEfectivo: number;
  totalCobradoTarjeta: number;
  totalCobradoPoint: number;
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

export interface ProductoCaja {
  id: number;
  categoriaId: number | null;
  sku: string;
  codigoBarras: string | null;
  nombre: string;
  imagenUrl: string | null;
  precio: number;
  moneda: string;
  controlaInventario: boolean;
  disponible: number | null;
}

export interface CatalogoVentaCaja { productos: ProductoCaja[]; }
export interface ProductoVentaPayload { productoId: number; cantidad: number; }
export interface RegistrarVentaPayload {
  citaIds?: number[];
  ventaPendienteId?: number | null;
  sucursalId: number;
  citaId: number | null;
  clienteId: number | null;
  productos: ProductoVentaPayload[];
  metodoPago: string;
  referencia: string | null;
  observaciones: string | null;
}
export interface DetalleVentaCaja {
  id: number; tipoConcepto: 'SERVICIO' | 'PRODUCTO'; productoId: number | null; servicioId: number | null;
  citaId: number | null; sku: string | null; nombre: string; cantidad: number; precioUnitario: number;
  descuento: number; impuestos: number; total: number;
}
export interface VentaCaja {
  id: number; folio: string; sucursalId: number; citaId: number | null; clienteId: number | null;
  estado: string; moneda: string; subtotal: number; descuentoTotal: number; impuestosTotal: number;
  total: number; metodoPago: string; referencia: string | null; creadaEn: string; canal: 'CAJA' | 'ONLINE'; detalles: DetalleVentaCaja[];
}

export interface TerminalPointCaja { id: number; terminalId: string; nombre: string; modoOperacion: string | null; sucursalId: number; sucursalNombre: string; activa: boolean; }
export interface OrdenPointCaja {
  id: string; ventaId: number; folioVenta: string; terminalId: number; terminalNombre: string;
  estado: string; detalleEstado: string | null; importe: number; moneda: string;
  proveedorOrdenId: string | null; proveedorPagoId: string | null; creadoEn: string | null; actualizadoEn: string | null;
  simulacionDisponible: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class CajaService {
  private readonly http = inject(HttpClient);

  getCatalogo(sucursalId?: number | null): Observable<CatalogoCaja> {
    let params = new HttpParams();
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<CatalogoCaja>(`${environment.apiBaseUrl}/caja/catalogo`, { params });
  }

  getSesionActual(sucursalId?: number | null): Observable<CajaSesion | null> {
    let params = new HttpParams();
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http
      .get(`${environment.apiBaseUrl}/caja/sesiones/actual`, {
        params,
        responseType: 'text'
      })
      .pipe(
        map(response => {
          const body = response?.trim();
          return body ? JSON.parse(body) as CajaSesion : null;
        })
      );
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

  listarMovimientos(sucursalId?: number | null): Observable<MovimientoCaja[]> {
    let params = new HttpParams();
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<MovimientoCaja[]>(`${environment.apiBaseUrl}/caja/movimientos`, { params });
  }

  getResumen(sucursalId?: number | null): Observable<ResumenCaja> {
    let params = new HttpParams();
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<ResumenCaja>(`${environment.apiBaseUrl}/caja/resumen`, { params });
  }

  getCatalogoVenta(sucursalId: number): Observable<CatalogoVentaCaja> {
    return this.http.get<CatalogoVentaCaja>(`${environment.apiBaseUrl}/caja/ventas/catalogo`, {
      params: new HttpParams().set('sucursalId', sucursalId)
    });
  }

  cotizarProductos(sucursalId: number, productos: { productoId: number; cantidad: number }[]): Observable<{ subtotal: number; descuento: number; total: number; lineas: { productoId: number; precioUnitario: number; total: number }[] }> {
    return this.http.post<{ subtotal: number; descuento: number; total: number; lineas: { productoId: number; precioUnitario: number; total: number }[] }>(`${environment.apiBaseUrl}/caja/ventas/cotizacion`, { sucursalId, productos });
  }
  ventasPendientesStaff(sucursalId:number,pagina=0) {
    return this.http.get<{content:{venta:VentaCaja;clienteNombre:string;clienteTelefono:string}[];totalPages:number;totalElements:number}>(`${environment.apiBaseUrl}/caja/ventas/pendientes-staff`,{params:{sucursalId,pagina}});
  }
  cobrarVentaStaff(ventaId:number,metodoPago:string,referencia:string|null) {
    return this.http.post<VentaCaja>(`${environment.apiBaseUrl}/caja/ventas/${ventaId}/cobrar`,{metodoPago,referencia});
  }
  registrarVenta(payload: RegistrarVentaPayload): Observable<VentaCaja> {
    return this.http.post<VentaCaja>(`${environment.apiBaseUrl}/caja/ventas`, payload);
  }

  listarVentas(sucursalId: number): Observable<VentaCaja[]> {
    return this.http.get<VentaCaja[]>(`${environment.apiBaseUrl}/caja/ventas`, {
      params: new HttpParams().set('sucursalId', sucursalId)
    });
  }

  listarTerminalesPoint(sucursalId: number): Observable<TerminalPointCaja[]> {
    return this.http.get<TerminalPointCaja[]>(`${environment.apiBaseUrl}/caja/point/terminales`, {
      params: new HttpParams().set('sucursalId', sucursalId)
    });
  }

  crearOrdenPoint(terminalId: number, venta: RegistrarVentaPayload, idempotencia: string): Observable<OrdenPointCaja> {
    return this.http.post<OrdenPointCaja>(`${environment.apiBaseUrl}/caja/point/ordenes`, { terminalId, venta }, {
      headers: { 'Idempotency-Key': idempotencia }
    });
  }

  consultarOrdenPoint(id: string): Observable<OrdenPointCaja> {
    return this.http.get<OrdenPointCaja>(`${environment.apiBaseUrl}/caja/point/ordenes/${id}`);
  }

  listarOrdenesPoint(sucursalId: number): Observable<OrdenPointCaja[]> {
    return this.http.get<OrdenPointCaja[]>(`${environment.apiBaseUrl}/caja/point/ordenes`, {
      params: new HttpParams().set('sucursalId', sucursalId)
    });
  }

  cancelarOrdenPoint(id: string, idempotencia: string): Observable<OrdenPointCaja> {
    return this.http.post<OrdenPointCaja>(`${environment.apiBaseUrl}/caja/point/ordenes/${id}/cancelar`, {}, {
      headers: { 'Idempotency-Key': idempotencia }
    });
  }

  reembolsarOrdenPoint(id: string, idempotencia: string): Observable<OrdenPointCaja> {
    return this.http.post<OrdenPointCaja>(`${environment.apiBaseUrl}/caja/point/ordenes/${id}/reembolsar`, {}, {
      headers: { 'Idempotency-Key': idempotencia }
    });
  }

  simularAprobacionPoint(id: string): Observable<OrdenPointCaja> {
    return this.http.post<OrdenPointCaja>(`${environment.apiBaseUrl}/caja/point/ordenes/${id}/simular-aprobacion`, {});
  }
}
