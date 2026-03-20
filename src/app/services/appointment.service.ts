import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, throwError } from 'rxjs';
import { timeout, retry, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface FranjaDisponible {
  inicio: string;
  fin: string;
  hora: string;
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
  private readonly legacyUrl = environment.legacyScriptUrl;
  private readonly token = environment.legacyToken;
  private readonly TIMEOUT_DURATION = 15000;
  private readonly RETRY_COUNT = 3;

  constructor(private http: HttpClient) {}

  getAvailableSlots(request: ConsultaFranjasRequest): Observable<FranjaDisponible[]> {
    if (environment.useBackendBooking && this.apiUrl) {
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
            if (environment.allowLegacyFallback) {
              return this.http.get(`${this.legacyUrl}?fecha=${request.fecha}`, { responseType: 'text' }).pipe(
                map(response => this.mapLegacySlots(request.fecha, response))
              );
            }
            return throwError(() => new Error('No se pudieron obtener las franjas disponibles.'));
          })
        );
    }

    return this.http.get(`${this.legacyUrl}?fecha=${request.fecha}`, { responseType: 'text' }).pipe(
      map(response => this.mapLegacySlots(request.fecha, response)),
      timeout(this.TIMEOUT_DURATION),
      retry(this.RETRY_COUNT),
      catchError(err => {
        console.error('Error obteniendo horarios legacy:', err);
        return throwError(() => new Error('Error al conectar con el servidor tras varios reintentos.'));
      })
    );
  }

  bookAppointment(data: CrearCitaBackendRequest): Observable<any> {
    if (environment.useBackendBooking && this.apiUrl) {
      return this.http.post(`${this.apiUrl}/publico/citas`, data).pipe(
        timeout(this.TIMEOUT_DURATION),
        retry(this.RETRY_COUNT),
        catchError(err => {
          console.error('Error al crear cita en backend:', err);
          if (environment.allowLegacyFallback) {
            const legacyDate = data.inicio.split('T')[0];
            const legacyHour = this.extractHour(data.inicio);
            const payload = {
              token: this.token,
              nombre: data.nombreCliente,
              telefono: data.telefonoCliente,
              correo: data.correoCliente,
              servicio: String(data.servicioId),
              fecha: legacyDate,
              hora: legacyHour
            };

            return this.http.post(this.legacyUrl, JSON.stringify(payload), {
              headers: { 'Content-Type': 'text/plain' },
              responseType: 'text'
            });
          }
          return throwError(() => new Error(err?.error?.mensaje || 'No se pudo crear la cita en el backend.'));
        })
      );
    }

    const legacyDate = data.inicio.split('T')[0];
    const legacyHour = this.extractHour(data.inicio);
    const payload = {
      token: this.token,
      nombre: data.nombreCliente,
      telefono: data.telefonoCliente,
      correo: data.correoCliente,
      servicio: String(data.servicioId),
      fecha: legacyDate,
      hora: legacyHour
    };

    return this.http
      .post(this.legacyUrl, JSON.stringify(payload), {
        headers: { 'Content-Type': 'text/plain' },
        responseType: 'text'
      })
      .pipe(
        timeout(this.TIMEOUT_DURATION),
        retry(this.RETRY_COUNT),
        catchError(err => {
          console.error('Error al agendar cita legacy:', err);
          return throwError(
            () => new Error('No se pudo completar la reserva tras varios intentos. Por favor, verifica tu conexión.')
          );
        })
      );
  }

  private mapBackendSlot(slot: FranjaDisponibleBackendResponse): FranjaDisponible {
    return {
      inicio: slot.inicio,
      fin: slot.fin,
      hora: this.formatHour(slot.inicio)
    };
  }

  private mapLegacySlots(fecha: string, response: string): FranjaDisponible[] {
    let available: string[] = [];

    try {
      const cleanedResponse = typeof response === 'string' ? response.trim() : response;
      if (cleanedResponse && cleanedResponse !== '[]') {
        available = typeof cleanedResponse === 'string' ? JSON.parse(cleanedResponse) : cleanedResponse;
      }
    } catch (error) {
      console.warn('No se pudo parsear la respuesta legacy de horarios:', error);
    }

    return available.map(hour => ({
      inicio: `${fecha}T${hour}:00`,
      fin: `${fecha}T${hour}:00`,
      hora: hour
    }));
  }

  private formatHour(isoDateTime: string): string {
    const date = new Date(isoDateTime);
    return new Intl.DateTimeFormat('es-MX', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }).format(date);
  }

  private extractHour(isoDateTime: string): string {
    const date = new Date(isoDateTime);
    const hour = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hour}:${minutes}`;
  }
}
