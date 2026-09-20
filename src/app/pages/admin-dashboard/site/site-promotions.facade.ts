import { Injectable, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AdminService, ActualizarPromocionesSitioPayload } from '../../../core/admin/admin.service';

@Injectable()
export class SitePromotionsFacade {
  private readonly admin = inject(AdminService);

  cargar() {
    return forkJoin({
      promociones: this.admin.getPromocionesSitio(),
      servicios: this.admin.getServicios(),
      sucursales: this.admin.getSucursales()
    });
  }

  guardar(payload: ActualizarPromocionesSitioPayload) {
    return this.admin.actualizarPromocionesSitio(payload);
  }
}
