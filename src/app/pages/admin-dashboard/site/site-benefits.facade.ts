import { Injectable, inject } from '@angular/core';
import { ActualizarBeneficiosSitioPayload, AdminService } from '../../../core/admin/admin.service';

@Injectable()
export class SiteBenefitsFacade {
  private readonly admin = inject(AdminService);

  cargar() {
    return this.admin.getBeneficiosSitio();
  }

  guardar(payload: ActualizarBeneficiosSitioPayload) {
    return this.admin.actualizarBeneficiosSitio(payload);
  }
}
