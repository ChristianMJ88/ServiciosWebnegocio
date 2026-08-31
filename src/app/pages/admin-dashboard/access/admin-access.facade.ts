import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminService, GuardarRolInternoPayload, GuardarUsuarioInternoPayload } from '../../../core/admin/admin.service';
import { construirPayloadRolInterno, construirPayloadUsuarioInterno } from './admin-access.helpers';

@Injectable({ providedIn: 'root' })
export class AdminAccessFacade {
  private readonly adminService = inject(AdminService);

  guardarUsuario(id: number | null, formulario: GuardarUsuarioInternoPayload): Observable<unknown> {
    const payload = construirPayloadUsuarioInterno(formulario);
    return id
      ? this.adminService.actualizarUsuarioInterno(id, payload)
      : this.adminService.invitarUsuarioInterno(payload);
  }

  guardarRol(id: number | null, clonarDesdeId: number | null, formulario: GuardarRolInternoPayload) {
    const payload = construirPayloadRolInterno(formulario);
    if (id) {
      return this.adminService.actualizarRolInterno(id, payload);
    }
    return clonarDesdeId
      ? this.adminService.clonarRolInterno(clonarDesdeId, payload)
      : this.adminService.crearRolInterno(payload);
  }

  eliminarRol(id: number) {
    return this.adminService.eliminarRolInterno(id);
  }
}
