import { Injectable, inject } from '@angular/core';
import { ActualizarPreguntasFrecuentesSitioPayload, AdminService } from '../../../core/admin/admin.service';

@Injectable()
export class SiteFaqFacade {
  private readonly admin = inject(AdminService);
  cargar() { return this.admin.getPreguntasFrecuentesSitio(); }
  guardar(payload: ActualizarPreguntasFrecuentesSitioPayload) {
    return this.admin.actualizarPreguntasFrecuentesSitio(payload);
  }
}
