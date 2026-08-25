import { Injectable, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import {
  AdminService,
  GuardarGrupoServicioPayload,
  GuardarServicioPayload,
  GuardarSubgrupoServicioPayload,
  GuardarSucursalPayload,
  GrupoServicioAdmin,
  ServicioAdmin,
  SubgrupoServicioAdmin
} from '../../../core/admin/admin.service';
import {
  construirPayloadGrupoServicio,
  construirPayloadServicio,
  construirPayloadSubgrupoServicio,
  construirPayloadSucursal
} from './admin-catalog.helpers';

@Injectable({ providedIn: 'root' })
export class AdminCatalogFacade {
  private readonly adminService = inject(AdminService);

  guardarSucursal(id: number | null, formulario: GuardarSucursalPayload) {
    const payload = construirPayloadSucursal(formulario);
    return id
      ? this.adminService.actualizarSucursal(id, payload)
      : this.adminService.crearSucursal(payload);
  }

  guardarGrupo(id: number | null, formulario: GuardarGrupoServicioPayload) {
    const payload = construirPayloadGrupoServicio(formulario);
    return id
      ? this.adminService.actualizarGrupoServicio(id, payload)
      : this.adminService.crearGrupoServicio(payload);
  }

  guardarSubgrupo(id: number | null, formulario: GuardarSubgrupoServicioPayload) {
    const payload = construirPayloadSubgrupoServicio(formulario);
    return id
      ? this.adminService.actualizarSubgrupoServicio(id, payload)
      : this.adminService.crearSubgrupoServicio(payload);
  }

  guardarServicio(id: number | null, formulario: GuardarServicioPayload) {
    const payload = construirPayloadServicio(formulario);
    return id
      ? this.adminService.actualizarServicio(id, payload)
      : this.adminService.crearServicio(payload);
  }

  alternarGrupo(grupo: GrupoServicioAdmin) {
    return this.adminService.actualizarGrupoServicio(grupo.id, {
      ...construirPayloadGrupoServicio(grupo),
      activo: !grupo.activo
    });
  }

  alternarSubgrupo(subgrupo: SubgrupoServicioAdmin) {
    return this.adminService.actualizarSubgrupoServicio(subgrupo.id, {
      ...construirPayloadSubgrupoServicio(subgrupo),
      activo: !subgrupo.activo
    });
  }

  alternarServicio(servicio: ServicioAdmin) {
    return this.adminService.actualizarServicio(servicio.id, {
      ...construirPayloadServicio(servicio),
      activo: !servicio.activo
    });
  }

  guardarOrdenGrupos(grupos: GrupoServicioAdmin[]) {
    return forkJoin(grupos.map(grupo =>
      this.adminService.actualizarGrupoServicio(grupo.id, construirPayloadGrupoServicio(grupo))
    ));
  }

  guardarOrdenSubgrupos(subgrupos: SubgrupoServicioAdmin[]) {
    return forkJoin(subgrupos.map(subgrupo =>
      this.adminService.actualizarSubgrupoServicio(subgrupo.id, construirPayloadSubgrupoServicio(subgrupo))
    ));
  }

  guardarOrdenServicios(servicios: ServicioAdmin[]) {
    return forkJoin(servicios.map(servicio =>
      this.adminService.actualizarServicio(servicio.id, construirPayloadServicio(servicio))
    ));
  }
}
