import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminService, GrupoServicioAdmin } from '../../../core/admin/admin.service';
import { AdminCatalogFacade } from './admin-catalog.facade';
import { crearFormularioSucursal } from './admin-catalog.helpers';

describe('AdminCatalogFacade', () => {
  const adminService = {
    crearSucursal: vi.fn(),
    actualizarSucursal: vi.fn(),
    actualizarGrupoServicio: vi.fn()
  };

  let facade: AdminCatalogFacade;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        AdminCatalogFacade,
        { provide: AdminService, useValue: adminService }
      ]
    });
    facade = TestBed.inject(AdminCatalogFacade);
  });

  it('elige crear o actualizar según exista un id', async () => {
    const formulario = crearFormularioSucursal();
    adminService.crearSucursal.mockReturnValue(of({ id: 1 }));
    adminService.actualizarSucursal.mockReturnValue(of({ id: 9 }));

    await firstValueFrom(facade.guardarSucursal(null, formulario));
    await firstValueFrom(facade.guardarSucursal(9, formulario));

    expect(adminService.crearSucursal).toHaveBeenCalledOnce();
    expect(adminService.actualizarSucursal).toHaveBeenCalledWith(9, expect.any(Object));
  });

  it('invierte el estado al actualizar un grupo', async () => {
    const grupo = {
      id: 4,
      nombre: 'Spa',
      slug: 'spa',
      descripcion: null,
      imagenUrl: null,
      icono: null,
      ordenPublico: 10,
      activo: true
    } as GrupoServicioAdmin;
    adminService.actualizarGrupoServicio.mockReturnValue(of({ ...grupo, activo: false }));

    await firstValueFrom(facade.alternarGrupo(grupo));

    expect(adminService.actualizarGrupoServicio)
      .toHaveBeenCalledWith(4, expect.objectContaining({ activo: false }));
  });

  it('persiste todos los grupos reordenados', async () => {
    const grupos = [1, 2].map((id, index) => ({
      id,
      nombre: `Grupo ${id}`,
      slug: `grupo-${id}`,
      descripcion: null,
      imagenUrl: null,
      icono: null,
      ordenPublico: index,
      activo: true
    } as GrupoServicioAdmin));
    adminService.actualizarGrupoServicio.mockImplementation((id: number) => of({ id }));

    await firstValueFrom(facade.guardarOrdenGrupos(grupos));

    expect(adminService.actualizarGrupoServicio).toHaveBeenCalledTimes(2);
  });
});
