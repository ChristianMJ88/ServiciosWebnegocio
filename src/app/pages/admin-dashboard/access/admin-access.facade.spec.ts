import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../../../core/admin/admin.service';
import { AdminAccessFacade } from './admin-access.facade';
import { crearFormularioRolInterno, crearFormularioUsuarioInterno } from './admin-access.helpers';

describe('AdminAccessFacade', () => {
  const adminService = {
    crearUsuarioInterno: vi.fn(),
    actualizarUsuarioInterno: vi.fn(),
    crearRolInterno: vi.fn(),
    actualizarRolInterno: vi.fn(),
    clonarRolInterno: vi.fn(),
    eliminarRolInterno: vi.fn()
  };

  let facade: AdminAccessFacade;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        AdminAccessFacade,
        { provide: AdminService, useValue: adminService }
      ]
    });
    facade = TestBed.inject(AdminAccessFacade);
  });

  it('delega la creación y actualización de usuarios', async () => {
    const formulario = crearFormularioUsuarioInterno(null, 3);
    adminService.crearUsuarioInterno.mockReturnValue(of({ usuarioId: 1 }));
    adminService.actualizarUsuarioInterno.mockReturnValue(of({ usuarioId: 7 }));

    await firstValueFrom(facade.guardarUsuario(null, formulario));
    await firstValueFrom(facade.guardarUsuario(7, formulario));

    expect(adminService.crearUsuarioInterno).toHaveBeenCalledOnce();
    expect(adminService.actualizarUsuarioInterno).toHaveBeenCalledWith(7, expect.any(Object));
  });

  it('distingue crear, editar y clonar roles', async () => {
    const formulario = crearFormularioRolInterno();
    adminService.crearRolInterno.mockReturnValue(of({ id: 1 }));
    adminService.actualizarRolInterno.mockReturnValue(of({ id: 2 }));
    adminService.clonarRolInterno.mockReturnValue(of({ id: 3 }));

    await firstValueFrom(facade.guardarRol(null, null, formulario));
    await firstValueFrom(facade.guardarRol(2, null, formulario));
    await firstValueFrom(facade.guardarRol(null, 3, formulario));

    expect(adminService.crearRolInterno).toHaveBeenCalledOnce();
    expect(adminService.actualizarRolInterno).toHaveBeenCalledOnce();
    expect(adminService.clonarRolInterno).toHaveBeenCalledOnce();
  });

  it('delega la eliminación sin decidir si el rol está autorizado', async () => {
    adminService.eliminarRolInterno.mockReturnValue(of(undefined));
    await firstValueFrom(facade.eliminarRol(5));
    expect(adminService.eliminarRolInterno).toHaveBeenCalledWith(5);
  });
});
