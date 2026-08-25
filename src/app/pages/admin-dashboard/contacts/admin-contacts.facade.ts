import { Injectable, inject } from '@angular/core';
import { AdminService } from '../../../core/admin/admin.service';

@Injectable({ providedIn: 'root' })
export class AdminContactsFacade {
  private readonly adminService = inject(AdminService);

  actualizarEstado(contactoId: number, estado: string) {
    return this.adminService.actualizarEstadoContacto(contactoId, estado);
  }
}
