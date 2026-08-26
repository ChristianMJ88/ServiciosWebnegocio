import { Injectable, inject } from '@angular/core';
import {
  CrearCitaRecepcionPayload,
  CrearSolicitudEsperaRecepcionPayload,
  RecepcionService
} from '../../../core/recepcion/recepcion.service';

@Injectable({ providedIn: 'root' })
export class RecepcionQuickAppointmentFacade {
  private readonly recepcionService = inject(RecepcionService);

  buscarClientes(texto: string) {
    return this.recepcionService.buscarClientes(texto.trim());
  }

  cargarFranjas(sucursalId: number, servicioId: number, fecha: string) {
    return this.recepcionService.getFranjasDisponibles(sucursalId, servicioId, fecha);
  }

  crearCita(payload: CrearCitaRecepcionPayload) {
    return this.recepcionService.crearCita(payload);
  }

  registrarEspera(payload: CrearSolicitudEsperaRecepcionPayload) {
    return this.recepcionService.registrarEspera(payload);
  }
}
