import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { timeout, retry, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private apiUrl = 'https://script.google.com/macros/s/AKfycbx1VZhYuxQ8yMXv0jBJpnYTSuNbOKj9jwd_SXyi_u9eBqsNY17PwynCN6dRfoWFrH_BeA/exec';


  private token = 'AGENDA2025';
  private TIMEOUT_DURATION = 15000; // 15 segundos
  private RETRY_COUNT = 3;

  constructor(private http: HttpClient) {}


  getAvailableSlots(fecha: string): Observable<string> {
    return this.http.get(`${this.apiUrl}?fecha=${fecha}`, { responseType: 'text' }).pipe(
      timeout(this.TIMEOUT_DURATION),
      retry(this.RETRY_COUNT),
      catchError(err => {
        console.error('Error obteniendo horarios:', err);
        return throwError(() => new Error('Error al conectar con el servidor tras varios reintentos.'));
      })
    );
  }

  bookAppointment(data: any): Observable<any> {
    // Ya incluimos el token en formData en el componente, pero por seguridad
    // nos aseguramos de que esté aquí también.
    const payload = {
      ...data,
      token: data.token || this.token
    };
    // Usamos 'text/plain' sin charset para asegurar que sea una "simple request"
    // y evitar el preflight OPTIONS que Google Apps Script no maneja bien.
    return this.http.post(this.apiUrl, JSON.stringify(payload), {
      headers: { 'Content-Type': 'text/plain' },
      responseType: 'text'
    }).pipe(
      timeout(this.TIMEOUT_DURATION),
      retry(this.RETRY_COUNT),
      catchError(err => {
        console.error('Error al agendar cita:', err);
        return throwError(() => new Error('No se pudo completar la reserva tras varios intentos. Por favor, verifica tu conexión.'));
      })
    );
  }
}
