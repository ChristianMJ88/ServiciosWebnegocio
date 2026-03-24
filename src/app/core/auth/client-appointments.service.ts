import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CitaCliente {
  id: number;
  estado: string;
  sucursalId: number;
  servicioId: number;
  prestadorId: number;
  sucursalNombre: string;
  servicioNombre: string;
  prestadorNombre: string;
  inicio: string;
  fin: string;
  precio: number;
  moneda: string;
  notas: string | null;
  cancelable: boolean;
  clienteNombre?: string | null;
  clienteCorreo?: string | null;
  clienteTelefono?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ClientAppointmentsService {
  private readonly http = inject(HttpClient);

  getMyAppointments(): Observable<CitaCliente[]> {
    return this.http.get<CitaCliente[]>(`${environment.apiBaseUrl}/cliente/citas`);
  }

  cancelAppointment(citaId: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/cliente/citas/${citaId}/cancelar`, {});
  }

  rescheduleAppointment(citaId: number, nuevoInicio: string): Observable<CitaCliente> {
    return this.http.patch<CitaCliente>(`${environment.apiBaseUrl}/cliente/citas/${citaId}/reprogramar`, {
      nuevoInicio
    });
  }
}
