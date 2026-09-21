import { Injectable, inject } from '@angular/core';
import { ActualizarResenasSitioPayload, AdminService } from '../../../core/admin/admin.service';

@Injectable()
export class SiteReviewsFacade {
  private readonly admin = inject(AdminService);

  cargar() {
    return this.admin.getResenasSitio();
  }

  guardar(payload: ActualizarResenasSitioPayload) {
    return this.admin.actualizarResenasSitio(payload);
  }
}
