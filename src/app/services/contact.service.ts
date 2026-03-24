import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface CrearSolicitudContactoRequest {
  empresaId: number;
  nombreCompleto: string;
  telefono?: string | null;
  correo: string;
  asunto: string;
  mensaje: string;
}

export interface SolicitudContactoCreadaResponse {
  id: number;
  empresaId: number;
  estado: string;
  mensaje: string;
  correoNotificacionProgramado: boolean;
  creadaEn: string;
}

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private readonly apiUrl = environment.apiBaseUrl;
  private readonly timeoutMs = 15000;

  constructor(private http: HttpClient) {}

  sendContact(request: CrearSolicitudContactoRequest): Observable<SolicitudContactoCreadaResponse> {
    if (!this.apiUrl) {
      return throwError(() => new Error('No hay un backend configurado para recibir mensajes.'));
    }

    return this.http.post<SolicitudContactoCreadaResponse>(`${this.apiUrl}/publico/contactos`, request).pipe(
      timeout(this.timeoutMs),
      catchError(err => throwError(() => new Error(this.resolveErrorMessage(err))))
    );
  }

  private resolveErrorMessage(error: any): string {
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

    return 'No se pudo enviar tu mensaje en este momento.';
  }
}
