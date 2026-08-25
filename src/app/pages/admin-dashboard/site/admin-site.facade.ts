import { Injectable, inject } from '@angular/core';
import { catchError, map, of, switchMap } from 'rxjs';
import { AdminService, GuardarConfiguracionSitioPayload } from '../../../core/admin/admin.service';
import { construirPayloadSitio } from './admin-site.forms';

@Injectable({ providedIn: 'root' })
export class AdminSiteFacade {
  private readonly adminService = inject(AdminService);

  guardar(formulario: GuardarConfiguracionSitioPayload) {
    return this.adminService.actualizarConfiguracionSitio(construirPayloadSitio(formulario)).pipe(
      switchMap(configuracion => this.adminService.getAuditoriaConfiguracion().pipe(
        map(auditoria => ({ configuracion, auditoria })),
        catchError(() => of({ configuracion, auditoria: null }))
      ))
    );
  }
}
