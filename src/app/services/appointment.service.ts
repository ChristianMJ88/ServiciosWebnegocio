import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, of, throwError } from 'rxjs';
import { timeout, retry, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

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
  empresaId: number;
  sucursalId: number;
  nombreCliente: string;
  correoCliente: string;
  telefonoCliente: string;
  notas?: string | null;
  items: CrearCitaMultipleItemBackendRequest[];
}

export interface CitasMultiplesCreadasResponse {
  citas: Array<{ id: number; mensaje?: string | null }>;
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

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private readonly apiUrl = environment.apiBaseUrl;
  private readonly TIMEOUT_DURATION = 15000;
  private readonly RETRY_COUNT = 3;

  constructor(private http: HttpClient) {}

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
      .get<FranjaDisponibleBackendResponse[]>(`${this.apiUrl}/publico/disponibilidad/franjas`, { params })
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

  bookAppointment(data: CrearCitaBackendRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/publico/citas`, data).pipe(
      timeout(this.TIMEOUT_DURATION),
      catchError(err => {
        console.error('Error al crear cita en backend:', err);
        return throwError(() => new Error(this.resolveBackendErrorMessage(err)));
      })
    );
  }

  bookMultipleAppointments(data: CrearCitasMultiplesBackendRequest): Observable<CitasMultiplesCreadasResponse> {
    return this.http.post<CitasMultiplesCreadasResponse>(`${this.apiUrl}/publico/citas/multiples`, data).pipe(
      timeout(this.TIMEOUT_DURATION),
      catchError(err => {
        console.error('Error al crear citas múltiples en backend:', err);
        return throwError(() => new Error(this.resolveBackendErrorMessage(err)));
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

    return this.http.get<PrestadorPublico[]>(`${this.apiUrl}/publico/servicios/prestadores`, { params }).pipe(
      timeout(this.TIMEOUT_DURATION),
      catchError(err => {
        console.error('Error obteniendo staff público:', err);
        return of([]);
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

    return 'No se pudo crear la cita en el backend.';
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
