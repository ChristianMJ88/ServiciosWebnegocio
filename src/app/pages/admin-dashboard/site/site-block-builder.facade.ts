import { Injectable, inject } from '@angular/core';
import { AdminService, BloqueSitioAdmin } from '../../../core/admin/admin.service';

@Injectable()
export class SiteBlockBuilderFacade {
  private readonly adminService = inject(AdminService);

  cargar() {
    return this.adminService.getBloquesSitio();
  }

  guardar(bloques: BloqueSitioAdmin[]) {
    return this.adminService.actualizarBloquesSitio({
      bloques: bloques.map((bloque, orden) => ({
        tipo: bloque.tipo,
        habilitado: bloque.habilitado,
        orden,
        superficie: bloque.superficie,
        presentacion: bloque.presentacion,
        anchoContenido: bloque.anchoContenido,
        alineacion: bloque.alineacion,
        separador: bloque.separador,
        espaciado: bloque.espaciado,
        etiqueta: bloque.etiqueta?.trim() || null,
        titulo: bloque.titulo?.trim() || null,
        descripcionVisual: bloque.descripcionVisual?.trim() || null
      }))
    });
  }
}
