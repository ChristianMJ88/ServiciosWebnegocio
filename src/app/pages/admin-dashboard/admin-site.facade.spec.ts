import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../../core/admin/admin.service';
import { AdminSiteFacade } from './admin-site.facade';
import { crearFormularioSitio } from './admin-site.forms';

describe('AdminSiteFacade', () => {
  const adminService = {
    actualizarConfiguracionSitio: vi.fn(),
    getAuditoriaConfiguracion: vi.fn()
  };
  let facade: AdminSiteFacade;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [AdminSiteFacade, { provide: AdminService, useValue: adminService }]
    });
    facade = TestBed.inject(AdminSiteFacade);
  });

  it('guarda y devuelve la auditoría actualizada', async () => {
    adminService.actualizarConfiguracionSitio.mockReturnValue(of({ empresaId: 1 }));
    adminService.getAuditoriaConfiguracion.mockReturnValue(of([{ id: 2 }]));
    const respuesta = await firstValueFrom(facade.guardar(crearFormularioSitio()));
    expect(respuesta).toEqual({ configuracion: { empresaId: 1 }, auditoria: [{ id: 2 }] });
  });

  it('conserva el guardado aunque falle la recarga de auditoría', async () => {
    adminService.actualizarConfiguracionSitio.mockReturnValue(of({ empresaId: 1 }));
    adminService.getAuditoriaConfiguracion.mockReturnValue(throwError(() => new Error('sin auditoría')));
    const respuesta = await firstValueFrom(facade.guardar(crearFormularioSitio()));
    expect(respuesta).toEqual({ configuracion: { empresaId: 1 }, auditoria: null });
  });
});
