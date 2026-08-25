import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../../../core/admin/admin.service';
import { AdminEmailFacade } from './admin-email.facade';
import { crearFormularioCorreo } from './admin-email.forms';

describe('AdminEmailFacade', () => {
  const adminService = {
    actualizarConfiguracionCorreo: vi.fn(),
    migrarSecretosCorreo: vi.fn(),
    getConfiguracionCorreo: vi.fn()
  };
  let facade: AdminEmailFacade;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [AdminEmailFacade, { provide: AdminService, useValue: adminService }]
    });
    facade = TestBed.inject(AdminEmailFacade);
  });

  it('normaliza y guarda la configuración', async () => {
    adminService.actualizarConfiguracionCorreo.mockReturnValue(of({ habilitado: true }));
    await firstValueFrom(facade.guardar({
      ...crearFormularioCorreo(),
      remitente: ' correo@example.com '
    }));

    expect(adminService.actualizarConfiguracionCorreo)
      .toHaveBeenCalledWith(expect.objectContaining({ remitente: 'correo@example.com' }));
  });

  it('recarga la configuración después de migrar secretos', async () => {
    adminService.migrarSecretosCorreo.mockReturnValue(of({ actualizada: true, mensaje: 'Ok' }));
    adminService.getConfiguracionCorreo.mockReturnValue(of({ habilitado: true }));

    const resultado = await firstValueFrom(facade.migrarSecretosYRecargar());

    expect(adminService.getConfiguracionCorreo).toHaveBeenCalledOnce();
    expect(resultado).toEqual({ resultado: { actualizada: true, mensaje: 'Ok' }, configuracion: { habilitado: true } });
  });
});
