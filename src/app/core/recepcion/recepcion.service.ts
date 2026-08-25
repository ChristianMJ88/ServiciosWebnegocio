import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SucursalRecepcionCatalogo {
  id: number;
  empresaId: number;
  nombre: string;
  direccion: string;
  telefono: string;
  zonaHoraria: string;
}

export interface ServicioRecepcionCatalogo {
  id: number;
  sucursalId: number;
  nombre: string;
  descripcion: string;
  duracionMinutos: number;
  bufferAntesMinutos: number;
  bufferDespuesMinutos: number;
  precio: number;
  moneda: string;
}

export interface CatalogoRecepcion {
  sucursalActivaId: number | null;
  sucursales: SucursalRecepcionCatalogo[];
  servicios: ServicioRecepcionCatalogo[];
  estadosCita: OpcionRecepcion[];
  estadoCitaPendiente: string;
  estadoCitaConfirmada: string;
  estadosCitaFinalizables: string[];
  estadosCitaCancelables: string[];
  estadosEspera: OpcionRecepcion[];
  estadoEsperaPendiente: string;
  estadoEsperaNotificada: string;
}

export interface OpcionRecepcion {
  codigo: string;
  etiqueta: string;
}

export interface FranjaRecepcionDisponible {
  inicio: string;
  fin: string;
  hora: string;
  prestadorId: number | null;
  servicioId: number;
  sucursalId: number;
  inicioAt: string;
  finAt: string;
}

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

export interface SolicitudEsperaRecepcion {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  servicioId: number;
  servicioNombre: string;
  clienteId: number | null;
  nombreCliente: string;
  telefonoCliente: string;
  fechaDeseada: string;
  horaDesde: string | null;
  horaHasta: string | null;
  aceptaWhatsapp: boolean;
  canalOrigen: string;
  estado: string;
  notas: string | null;
  creadaEn: string;
  notificadaEn: string | null;
  cerradoEn: string | null;
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

export interface CrearSolicitudEsperaRecepcionPayload {
  sucursalId: number;
  servicioId: number;
  clienteId: number | null;
  nombreCliente: string;
  telefonoCliente: string;
  fechaDeseada: string;
  horaDesde: string | null;
  horaHasta: string | null;
  aceptaWhatsapp: boolean;
  canalOrigen: string;
  notas: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class RecepcionService {
  private readonly http = inject(HttpClient);

  getCatalogo(sucursalId?: number | null): Observable<CatalogoRecepcion> {
    let params = new HttpParams();
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<CatalogoRecepcion>(`${environment.apiBaseUrl}/recepcion/catalogo`, { params });
  }

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

  getFranjasDisponibles(
    sucursalId: number,
    servicioId: number,
    fecha: string,
    prestadorId?: number | null
  ): Observable<FranjaRecepcionDisponible[]> {
    let params = new HttpParams()
      .set('sucursalId', sucursalId)
      .set('servicioId', servicioId)
      .set('fecha', fecha);

    if (prestadorId) {
      params = params.set('prestadorId', prestadorId);
    }

    return this.http.get<FranjaRecepcionDisponible[]>(`${environment.apiBaseUrl}/recepcion/franjas-disponibles`, { params });
  }

  crearCita(payload: CrearCitaRecepcionPayload): Observable<CitaCreadaRecepcion> {
    return this.http.post<CitaCreadaRecepcion>(`${environment.apiBaseUrl}/recepcion/citas`, payload);
  }

  getSolicitudesEspera(fecha?: string | null, sucursalId?: number | null): Observable<SolicitudEsperaRecepcion[]> {
    let params = new HttpParams();
    if (fecha) {
      params = params.set('fecha', fecha);
    }
    if (sucursalId) {
      params = params.set('sucursalId', sucursalId);
    }
    return this.http.get<SolicitudEsperaRecepcion[]>(`${environment.apiBaseUrl}/recepcion/espera`, { params });
  }

  registrarEspera(payload: CrearSolicitudEsperaRecepcionPayload): Observable<SolicitudEsperaRecepcion> {
    return this.http.post<SolicitudEsperaRecepcion>(`${environment.apiBaseUrl}/recepcion/espera`, payload);
  }

  notificarEspera(solicitudId: number): Observable<SolicitudEsperaRecepcion> {
    return this.http.patch<SolicitudEsperaRecepcion>(`${environment.apiBaseUrl}/recepcion/espera/${solicitudId}/notificar`, {});
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
