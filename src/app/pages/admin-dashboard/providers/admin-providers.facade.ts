import { Observable } from 'rxjs';
import { Injectable, inject } from '@angular/core';
import { AdminService, GuardarPrestadorPayload } from '../../../core/admin/admin.service';
import { construirPayloadPrestador } from './admin-providers.forms';

@Injectable({ providedIn: 'root' })
export class AdminProvidersFacade {
  private readonly adminService = inject(AdminService);

  impacto(id: number) { return this.adminService.getImpactoPrestador(id); }

  cargarConfiguracionMedia() {
    return this.adminService.getConfiguracionMediaPrestadores();
  }

  subirFoto(archivo: File) {
    return this.adminService.subirImagenPrestador(archivo);
  }

  guardar(id: number | null, formulario: GuardarPrestadorPayload): Observable<unknown> {
    const payload = construirPayloadPrestador(formulario);
    return id
      ? this.adminService.actualizarPrestador(id, payload)
      : payload.nombreUsuario ? this.adminService.crearPrestador(payload) : this.adminService.invitarPrestador(payload);
  }
}
