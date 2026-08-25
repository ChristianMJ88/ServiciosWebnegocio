import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../../core/admin/admin.service';
import { AdminAvailabilityFacade } from './admin-availability.facade';
import { crearFormularioExcepcion, crearFormularioRegla } from './admin-availability.forms';

describe('AdminAvailabilityFacade', () => {
  const adminService = {
    crearReglaDisponibilidad: vi.fn(),
    actualizarReglaDisponibilidad: vi.fn(),
    crearExcepcionDisponibilidad: vi.fn(),
    actualizarExcepcionDisponibilidad: vi.fn()
  };

  let facade: AdminAvailabilityFacade;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        AdminAvailabilityFacade,
        { provide: AdminService, useValue: adminService }
      ]
    });
    facade = TestBed.inject(AdminAvailabilityFacade);
  });

  it('elige crear o actualizar una regla según exista un id', async () => {
    adminService.crearReglaDisponibilidad.mockReturnValue(of({ id: 1 }));
    adminService.actualizarReglaDisponibilidad.mockReturnValue(of({ id: 7 }));

    await firstValueFrom(facade.guardarRegla(null, crearFormularioRegla()));
    await firstValueFrom(facade.guardarRegla(7, crearFormularioRegla()));

    expect(adminService.crearReglaDisponibilidad).toHaveBeenCalledOnce();
    expect(adminService.actualizarReglaDisponibilidad).toHaveBeenCalledWith(7, expect.any(Object));
  });

  it('elige crear o actualizar una excepción según exista un id', async () => {
    adminService.crearExcepcionDisponibilidad.mockReturnValue(of({ id: 2 }));
    adminService.actualizarExcepcionDisponibilidad.mockReturnValue(of({ id: 9 }));

    await firstValueFrom(facade.guardarExcepcion(null, crearFormularioExcepcion()));
    await firstValueFrom(facade.guardarExcepcion(9, crearFormularioExcepcion()));

    expect(adminService.crearExcepcionDisponibilidad).toHaveBeenCalledOnce();
    expect(adminService.actualizarExcepcionDisponibilidad).toHaveBeenCalledWith(9, expect.any(Object));
  });
});
