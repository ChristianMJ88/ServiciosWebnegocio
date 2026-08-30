import { Injectable, inject } from '@angular/core';
import { switchMap, map } from 'rxjs/operators';
import { AdminService, GuardarConfiguracionCorreoPayload } from '../../../core/admin/admin.service';
import { construirPayloadCorreo } from './admin-email.forms';

@Injectable({ providedIn: 'root' })
export class AdminEmailFacade {
  private readonly adminService = inject(AdminService);

  guardar(formulario: GuardarConfiguracionCorreoPayload) {
    return this.adminService.actualizarConfiguracionCorreo(construirPayloadCorreo(formulario));
  }

  usarCorreoPlataforma() {
    return this.adminService.usarCorreoPlataforma();
  }

  migrarSecretosYRecargar() {
    return this.adminService.migrarSecretosCorreo().pipe(
      switchMap(resultado => this.adminService.getConfiguracionCorreo().pipe(
        map(configuracion => ({ resultado, configuracion }))
      ))
    );
  }
}
