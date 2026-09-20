import { Injectable, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ActualizarGaleriaSitioPayload, AdminService } from '../../../core/admin/admin.service';

@Injectable()
export class SiteGalleryFacade {
  private readonly admin = inject(AdminService);

  cargar() {
    return forkJoin({
      galeria: this.admin.getGaleriaSitio(),
      servicios: this.admin.getServicios(),
      sucursales: this.admin.getSucursales(),
      media: this.admin.getConfiguracionMediaSitio()
    });
  }

  guardar(payload: ActualizarGaleriaSitioPayload) {
    return this.admin.actualizarGaleriaSitio(payload);
  }

  subirImagen(archivo: File) {
    return this.admin.subirImagenGaleriaSitio(archivo);
  }
}
