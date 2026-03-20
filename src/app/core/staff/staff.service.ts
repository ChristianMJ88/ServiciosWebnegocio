import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CitaStaff {
  id: number;
  estado: string;
  sucursalNombre: string;
  servicioNombre: string;
  clienteNombre: string;
  clienteCorreo: string;
  clienteTelefono: string;
  inicio: string;
  fin: string;
  notas: string | null;
}

export interface ReglaDisponibilidadStaff {
  id: number;
  tipoSujeto: string;
  sujetoId: number;
  sujetoNombre: string;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  intervaloMinutos: number;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
}

export interface GuardarReglaDisponibilidadStaffPayload {
  tipoSujeto: string;
  sujetoId: number;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  intervaloMinutos: number;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
}

export interface ExcepcionDisponibilidadStaff {
  id: number;
  tipoSujeto: string;
  sujetoId: number;
  sujetoNombre: string;
  fechaExcepcion: string;
  horaInicio: string | null;
  horaFin: string | null;
  tipoBloqueo: string;
  motivo: string | null;
}

export interface GuardarExcepcionDisponibilidadStaffPayload {
  tipoSujeto: string;
  sujetoId: number;
  fechaExcepcion: string;
  horaInicio: string | null;
  horaFin: string | null;
  tipoBloqueo: string;
  motivo: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class StaffService {
  private readonly http = inject(HttpClient);

  getAgenda(): Observable<CitaStaff[]> {
    return this.http.get<CitaStaff[]>(`${environment.apiBaseUrl}/staff/agenda`);
  }

  confirmar(citaId: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/staff/citas/${citaId}/confirmar`, {});
  }

  finalizar(citaId: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/staff/citas/${citaId}/finalizar`, {});
  }

  noAsistio(citaId: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiBaseUrl}/staff/citas/${citaId}/no-asistio`, {});
  }

  getReglasDisponibilidad(): Observable<ReglaDisponibilidadStaff[]> {
    return this.http.get<ReglaDisponibilidadStaff[]>(`${environment.apiBaseUrl}/staff/disponibilidad/reglas`);
  }

  crearReglaDisponibilidad(payload: GuardarReglaDisponibilidadStaffPayload): Observable<ReglaDisponibilidadStaff> {
    return this.http.post<ReglaDisponibilidadStaff>(`${environment.apiBaseUrl}/staff/disponibilidad/reglas`, payload);
  }

  actualizarReglaDisponibilidad(id: number, payload: GuardarReglaDisponibilidadStaffPayload): Observable<ReglaDisponibilidadStaff> {
    return this.http.patch<ReglaDisponibilidadStaff>(`${environment.apiBaseUrl}/staff/disponibilidad/reglas/${id}`, payload);
  }

  getExcepcionesDisponibilidad(): Observable<ExcepcionDisponibilidadStaff[]> {
    return this.http.get<ExcepcionDisponibilidadStaff[]>(`${environment.apiBaseUrl}/staff/disponibilidad/excepciones`);
  }

  crearExcepcionDisponibilidad(payload: GuardarExcepcionDisponibilidadStaffPayload): Observable<ExcepcionDisponibilidadStaff> {
    return this.http.post<ExcepcionDisponibilidadStaff>(`${environment.apiBaseUrl}/staff/disponibilidad/excepciones`, payload);
  }

  actualizarExcepcionDisponibilidad(id: number, payload: GuardarExcepcionDisponibilidadStaffPayload): Observable<ExcepcionDisponibilidadStaff> {
    return this.http.patch<ExcepcionDisponibilidadStaff>(`${environment.apiBaseUrl}/staff/disponibilidad/excepciones/${id}`, payload);
  }
}
