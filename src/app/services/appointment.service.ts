import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, of, throwError } from 'rxjs';
import { timeout, retry, catchError } from 'rxjs/operators';
import { TenantContextService } from '../core/tenant/tenant-context.service';
import { environment } from '../../environments/environment';

export interface DiasDisponibles {
  hoyLocal: string; reservaHasta: string; dias: string[];
  configuracion: { anticipacionMinimaMinutos:number; ventanaReservaDias:number; liberacionSinConfirmacionHoras:number; minutosApartadoPago:number };
}
export interface FranjaDisponible {
  inicio: string;
  fin: string;
  hora: string;
  prestadorId: number | null;
}

export interface PrestadorPublico {
  usuarioId: number;
  sucursalId: number;
  nombreMostrar: string;
  biografia: string | null;
  fotoUrl: string | null;
  colorAgenda: string | null;
}

export interface ConsultaFranjasRequest {
  empresaId: number;
  sucursalId: number;
  servicioId: number;
  prestadorId?: number | null;
  fecha: string;
}

export interface CrearCitaBackendRequest {
  promocionId?: number | null;
  empresaId: number;
  sucursalId: number;
  servicioId: number;
  prestadorId?: number | null;
  nombreCliente: string;
  correoCliente: string;
  telefonoCliente: string;
  inicio: string;
  notas?: string | null;
}

export interface CrearCitaMultipleItemBackendRequest {
  servicioId: number;
  prestadorId?: number | null;
  inicio: string;
}

export interface CrearCitasMultiplesBackendRequest {
  promocionId?: number | null;
  empresaId: number;
  sucursalId: number;
  nombreCliente: string;
  correoCliente: string;
  telefonoCliente: string;
  notas?: string | null;
  items: CrearCitaMultipleItemBackendRequest[];
}

export interface CitaCreadaBackendResponse {
  id: number;
  estado: string;
  mensaje: string;
  precio: number;
  precioLista: number;
  descuento: number;
  requierePago: boolean;
  importeRequerido: number;
  politicaCobro: string;
  pagoExpiraEn: string | null;
  confirmacionExpiraEn: string | null;
}
export interface CitasMultiplesCreadasResponse {
  citas: CitaCreadaBackendResponse[];
  total: number;
  mensaje: string;
}

interface FranjaDisponibleBackendResponse {
  inicio: string;
  fin: string;
  prestadorId: number;
  servicioId: number;
  sucursalId: number;
}

export interface CotizacionCitas {
  reservableEnLinea?: boolean; motivoNoReservable?: string | null;
  requierePago?:boolean; importeRequeridoTotal?:number; requierePagoProductos?:boolean; avisoProductos?:string|null;
  productos?:{productoId:number;nombre:string;cantidad:number;precioListaUnitario:number;precioUnitario:number;descuento:number;total:number;moneda:string}[];
  moneda: string; subtotalLista: number; descuentoTotal: number; total: number; anticipoTotal: number;
  promocion: { id: number; titulo: string; tipoDescuento: string; valorDescuento: number; moneda: string } | null;
  motivoNoAplicada: string | null;
  lineas: Array<{ servicioId: number; nombre: string; precioLista: number; descuento: number; precioNeto: number; anticipo: number; promocionAplicada: boolean; politicaCobro?:string; importeRequerido?:number; saldoEnNegocio?:number }>;
}
export interface CotizarCitasPayload {
  empresaId: number; sucursalId: number; promocionId: number | null;
  productos?:{productoId:number;cantidad:number}[];
  items: Array<{servicioId: number; inicio: string | null; fecha?: string | null}>;
}

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private readonly tenant = inject(TenantContextService);
  private publicHeaders() { return { 'X-Tenant-Slug': this.tenant.slug() ?? '' }; }
  private readonly apiUrl = environment.apiBaseUrl;
  private readonly TIMEOUT_DURATION = 15000;
  private readonly RETRY_COUNT = 3;

  constructor(private http: HttpClient) {}

  cotizarCitas(payload: CotizarCitasPayload): Observable<CotizacionCitas> {
    return this.http.post<CotizacionCitas>(`${this.apiUrl}/publico/citas/cotizar`, payload, {headers:this.publicHeaders()});
  }

getAvailableDays(sucursalId:number,servicioIds:number[],desde?:string,hasta?:string,prestadorIds?:(number|null)[]): Observable<DiasDisponibles> {
  let params=new HttpParams().set('sucursalId',sucursalId).set('servicioIds',servicioIds.join(','));
  if(desde) params=params.set('desde',desde);
  if(hasta) params=params.set('hasta',hasta);
  if(prestadorIds) params=params.set('prestadorIds',prestadorIds.map(id => id ?? 0).join(','));
  return this.http.get<DiasDisponibles>(`${this.apiUrl}/publico/disponibilidad/dias`,{params,headers:this.publicHeaders()});
}
  getAvailableSlots(request: ConsultaFranjasRequest): Observable<FranjaDisponible[]> {
    let params = new HttpParams()
      .set('empresaId', request.empresaId)
      .set('sucursalId', request.sucursalId)
      .set('servicioId', request.servicioId)
      .set('fecha', request.fecha);

    if (request.prestadorId) {
      params = params.set('prestadorId', request.prestadorId);
    }

    return this.http
      .get<FranjaDisponibleBackendResponse[]>(`${this.apiUrl}/publico/disponibilidad/franjas`, { params, headers: this.publicHeaders() })
      .pipe(
        map(response => response.map(slot => this.mapBackendSlot(slot))),
        timeout(this.TIMEOUT_DURATION),
        retry(this.RETRY_COUNT),
        catchError(err => {
          console.error('Error obteniendo horarios del backend:', err);
          return throwError(() => new Error('No se pudieron obtener las franjas disponibles.'));
        })
      );
  }

  bookAppointment(data: CrearCitaBackendRequest): Observable<CitaCreadaBackendResponse> {
    return this.http.post<CitaCreadaBackendResponse>(`${this.apiUrl}/publico/citas`, data, {headers:this.publicHeaders()}).pipe(
      timeout(this.TIMEOUT_DURATION),
      catchError(err => {
        console.error('Error al crear cita en backend:', err);
        return throwError(() => Object.assign(new Error(this.resolveBackendErrorMessage(err)), {status:err.status}));
      })
    );
  }

  bookMultipleAppointments(data: CrearCitasMultiplesBackendRequest): Observable<CitasMultiplesCreadasResponse> {
    return this.http.post<CitasMultiplesCreadasResponse>(`${this.apiUrl}/publico/citas/multiples`, data, {headers:this.publicHeaders()}).pipe(
      timeout(this.TIMEOUT_DURATION),
      catchError(err => {
        console.error('Error al crear citas múltiples en backend:', err);
        return throwError(() => Object.assign(new Error(this.resolveBackendErrorMessage(err)), {status:err.status}));
      })
    );
  }

  getPublicStaff(empresaId: number, sucursalId: number, servicioId?: number | null): Observable<PrestadorPublico[]> {
    let params = new HttpParams()
      .set('empresaId', empresaId)
      .set('sucursalId', sucursalId);

    if (servicioId) {
      params = params.set('servicioId', servicioId);
    }

    return this.http.get<PrestadorPublico[]>(`${this.apiUrl}/publico/servicios/prestadores`, { params, headers: this.publicHeaders() }).pipe(
      timeout(this.TIMEOUT_DURATION),
      catchError(err => {
        console.error('Error obteniendo staff público:', err);
        return throwError(() => new Error('No pudimos cargar los profesionales disponibles. Intenta de nuevo.'));
      })
    );
  }

  private mapBackendSlot(slot: FranjaDisponibleBackendResponse): FranjaDisponible {
    return {
      inicio: slot.inicio,
      fin: slot.fin,
      hora: this.formatHour(slot.inicio),
      prestadorId: slot.prestadorId
    };
  }

  private resolveBackendErrorMessage(error: any): string {
    const errorBody = error?.error;

    if (typeof errorBody === 'string' && errorBody.trim()) {
      return errorBody;
    }

    if (errorBody?.mensaje) {
      if (errorBody?.errores && typeof errorBody.errores === 'object') {
        const detalles = Object.values(errorBody.errores).join(' ');
        return detalles ? `${errorBody.mensaje}. ${detalles}` : errorBody.mensaje;
      }
      return errorBody.mensaje;
    }

    return errorBody?.message || 'No se pudo crear la cita en el backend.';
  }

  private formatHour(isoDateTime: string): string {
    const date = new Date(isoDateTime);
    return new Intl.DateTimeFormat('es-MX', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }).format(date);
  }

}
