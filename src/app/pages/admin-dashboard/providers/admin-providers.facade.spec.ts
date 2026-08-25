import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../../../core/admin/admin.service';
import { AdminProvidersFacade } from './admin-providers.facade';
import { crearFormularioPrestador } from './admin-providers.forms';

describe('AdminProvidersFacade', () => {
  const adminService = {
    crearPrestador: vi.fn(),
    actualizarPrestador: vi.fn()
  };
  let facade: AdminProvidersFacade;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [AdminProvidersFacade, { provide: AdminService, useValue: adminService }]
    });
    facade = TestBed.inject(AdminProvidersFacade);
  });

  it('elige crear o actualizar según exista un id', async () => {
    adminService.crearPrestador.mockReturnValue(of({ usuarioId: 1 }));
    adminService.actualizarPrestador.mockReturnValue(of({ usuarioId: 8 }));

    await firstValueFrom(facade.guardar(null, crearFormularioPrestador()));
    await firstValueFrom(facade.guardar(8, crearFormularioPrestador()));

    expect(adminService.crearPrestador).toHaveBeenCalledOnce();
    expect(adminService.actualizarPrestador).toHaveBeenCalledWith(8, expect.any(Object));
  });
});
