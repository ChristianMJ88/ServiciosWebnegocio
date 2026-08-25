import { Injectable, inject } from '@angular/core';
import {
  AdminService,
  GuardarExcepcionDisponibilidadPayload,
  GuardarReglaDisponibilidadPayload
} from '../../../core/admin/admin.service';
import { construirPayloadExcepcion, construirPayloadRegla } from './admin-availability.forms';

@Injectable({ providedIn: 'root' })
export class AdminAvailabilityFacade {
  private readonly adminService = inject(AdminService);

  guardarRegla(id: number | null, formulario: GuardarReglaDisponibilidadPayload) {
    const payload = construirPayloadRegla(formulario);
    return id
      ? this.adminService.actualizarReglaDisponibilidad(id, payload)
      : this.adminService.crearReglaDisponibilidad(payload);
  }

  guardarExcepcion(id: number | null, formulario: GuardarExcepcionDisponibilidadPayload) {
    const payload = construirPayloadExcepcion(formulario);
    return id
      ? this.adminService.actualizarExcepcionDisponibilidad(id, payload)
      : this.adminService.crearExcepcionDisponibilidad(payload);
  }
}
