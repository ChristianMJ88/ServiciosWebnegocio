import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AdminService,
  GuardarExcepcionDisponibilidadPayload,
  GuardarReglaDisponibilidadPayload
} from '../../../core/admin/admin.service';
import { construirPayloadExcepcion, construirPayloadRegla } from './admin-availability.forms';

@Injectable({ providedIn: 'root' })
export class AdminAvailabilityFacade {
  private readonly adminService = inject(AdminService);

  guardarRegla(id: number | null, formulario: GuardarReglaDisponibilidadPayload, semana?: {diasSemana:number[];reemplazar:boolean}): Observable<unknown> {
    const payload = construirPayloadRegla(formulario);
    if(!id && semana) return this.adminService.guardarHorarioSemanal(payload,semana.diasSemana,semana.reemplazar);
    return id
      ? this.adminService.actualizarReglaDisponibilidad(id, payload)
      : this.adminService.crearReglaDisponibilidad(payload);
  }

  eliminarRegla(id: number) { return this.adminService.eliminarReglaDisponibilidad(id); }

  guardarExcepcion(id: number | null, formulario: GuardarExcepcionDisponibilidadPayload) {
    const payload = construirPayloadExcepcion(formulario);
    return id
      ? this.adminService.actualizarExcepcionDisponibilidad(id, payload)
      : this.adminService.crearExcepcionDisponibilidad(payload);
  }
}
