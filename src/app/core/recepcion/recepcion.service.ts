import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CitaRecepcion {
  id: number;
  estado: string;
  sucursalId: number;
  servicioId: number;
  prestadorId: number;
  sucursalNombre: string;
  servicioNombre: string;
  prestadorNombre: string;
  clienteNombre: string;
  clienteCorreo: string;
  clienteTelefono: string;
  inicio: string;
  fin: string;
  precio: number;
  moneda: string;
  notas: string | null;
  checkInEn: string | null;
  checkInPorUsuarioId: number | null;
}

export interface ClienteRecepcion {
  usuarioId: number;
  nombreCompleto: string;
  telefono: string;
  correo: string;
  aceptaWhatsapp: boolean;
}

export interface CrearCitaRecepcionPayload {
  sucursalId: number;
  servicioId: number;
  prestadorId: number | null;
  nombreCliente: string;
  correoCliente: string;
  telefonoCliente: string;
  inicio: string;
  notas: string | null;
}

export interface CitaCreadaRecepcion {
  id: number;
  estado: string;
  empresaId: number;
  sucursalId: number;
  servicioId: number;
  prestadorId: number;
  inicio: string;
  fin: string;
  mensaje: string;
  correoConfirmacionProgramado: boolean;
  correoConfirmacionEnviado: boolean;
}

export interface CitaReprogramadaRecepcion {
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
  clienteNombre: string;
  clienteCorreo: string;
  clienteTelefono: string;
}

@Injectable({
  providedIn: 'root'
})
export class RecepcionService {
  private readonly http = inject(HttpClient);

  getAgenda(fecha?: string | null, sucursalId?: number | null): Observable<CitaRecepcion[]> {
    let params = new HttpParams();
    if (fecha) {
      params = params.set('fecha', fecha);
    }
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<CitaRecepcion[]>(`${environment.apiBaseUrl}/recepcion/agenda`, { params });
  }

  buscarClientes(texto: string): Observable<ClienteRecepcion[]> {
    const params = new HttpParams().set('texto', texto);
    return this.http.get<ClienteRecepcion[]>(`${environment.apiBaseUrl}/recepcion/clientes`, { params });
  }

  crearCita(payload: CrearCitaRecepcionPayload): Observable<CitaCreadaRecepcion> {
    return this.http.post<CitaCreadaRecepcion>(`${environment.apiBaseUrl}/recepcion/citas`, payload);
  }

  checkIn(citaId: number): Observable<CitaRecepcion> {
    return this.http.patch<CitaRecepcion>(`${environment.apiBaseUrl}/recepcion/citas/${citaId}/check-in`, {});
  }

  confirmar(citaId: number): Observable<CitaRecepcion> {
    return this.http.patch<CitaRecepcion>(`${environment.apiBaseUrl}/recepcion/citas/${citaId}/confirmar`, {});
  }

  cancelar(citaId: number): Observable<CitaRecepcion> {
    return this.http.patch<CitaRecepcion>(`${environment.apiBaseUrl}/recepcion/citas/${citaId}/cancelar`, {});
  }

  finalizar(citaId: number): Observable<CitaRecepcion> {
    return this.http.patch<CitaRecepcion>(`${environment.apiBaseUrl}/recepcion/citas/${citaId}/finalizar`, {});
  }

  reagendar(citaId: number, nuevoInicio: string): Observable<CitaReprogramadaRecepcion> {
    return this.http.patch<CitaReprogramadaRecepcion>(
      `${environment.apiBaseUrl}/recepcion/citas/${citaId}/reagendar`,
      { nuevoInicio }
    );
  }
}
