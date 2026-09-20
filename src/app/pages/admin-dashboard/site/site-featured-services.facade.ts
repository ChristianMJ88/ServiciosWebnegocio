import { Injectable, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import {
  AdminService,
  ActualizarServiciosDestacadosPayload
} from '../../../core/admin/admin.service';

@Injectable()
export class SiteFeaturedServicesFacade {
  private readonly adminService = inject(AdminService);

  cargar() {
    return forkJoin({
      configuracion: this.adminService.getServiciosDestacadosSitio(),
      grupos: this.adminService.getGruposServicio(),
      subgrupos: this.adminService.getSubgruposServicio(),
      servicios: this.adminService.getServicios()
    });
  }

  guardar(payload: ActualizarServiciosDestacadosPayload) {
    return this.adminService.actualizarServiciosDestacadosSitio(payload);
  }
}
