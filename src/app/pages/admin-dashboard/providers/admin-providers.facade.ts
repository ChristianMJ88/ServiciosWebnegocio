import { Injectable, inject } from '@angular/core';
import { AdminService, GuardarPrestadorPayload } from '../../../core/admin/admin.service';
import { construirPayloadPrestador } from './admin-providers.forms';

@Injectable({ providedIn: 'root' })
export class AdminProvidersFacade {
  private readonly adminService = inject(AdminService);

  guardar(id: number | null, formulario: GuardarPrestadorPayload) {
    const payload = construirPayloadPrestador(formulario);
    return id
      ? this.adminService.actualizarPrestador(id, payload)
      : this.adminService.crearPrestador(payload);
  }
}
