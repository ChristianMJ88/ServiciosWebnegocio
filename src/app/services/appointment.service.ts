import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private apiUrl = 'https://script.google.com/macros/s/AKfycbxxzwusLPH3Hh9xC9Fh2GaTaPh5-pPD50gLMsdgOIvWWBdFM6ylxRKn9lh9Kfz_ZNRxuQ/exec';


  private token = 'AGENDA2025';

  constructor(private http: HttpClient) {}


  getOccupiedSlots(fecha: string): Observable<string> {
    return this.http.get(`${this.apiUrl}?fecha=${fecha}`, { responseType: 'text' });
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
    });
  }
}
