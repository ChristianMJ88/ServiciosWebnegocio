import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminService, GuardarPlantillaWhatsappEmpresaPayload } from '../../../core/admin/admin.service';
import { AdminWhatsappFacade } from './admin-whatsapp.facade';

describe('AdminWhatsappFacade', () => {
  const adminService = {
    crearPlantillaWhatsappEmpresa: vi.fn(),
    actualizarPlantillaWhatsappEmpresa: vi.fn(),
    enviarMensajeWhatsapp: vi.fn(),
    detectarChannelSenderWhatsapp: vi.fn()
  };

  let facade: AdminWhatsappFacade;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        AdminWhatsappFacade,
        { provide: AdminService, useValue: adminService }
      ]
    });
    facade = TestBed.inject(AdminWhatsappFacade);
  });

  it('elige crear o actualizar plantillas por id', async () => {
    const payload = { nombre: 'Confirmación', uso: 'CITAS', contentSid: 'HX1' } as GuardarPlantillaWhatsappEmpresaPayload;
    adminService.crearPlantillaWhatsappEmpresa.mockReturnValue(of({ id: 1 }));
    adminService.actualizarPlantillaWhatsappEmpresa.mockReturnValue(of({ id: 4 }));

    await firstValueFrom(facade.guardarPlantilla(null, payload));
    await firstValueFrom(facade.guardarPlantilla(4, payload));

    expect(adminService.crearPlantillaWhatsappEmpresa).toHaveBeenCalledWith(payload);
    expect(adminService.actualizarPlantillaWhatsappEmpresa).toHaveBeenCalledWith(4, payload);
  });

  it('delega mensajes sin decidir contenido o destinatario', async () => {
    const payload = { telefono: '+525500000000', mensaje: 'Mensaje' };
    adminService.enviarMensajeWhatsapp.mockReturnValue(of({ id: 8 }));
    await firstValueFrom(facade.enviarMensaje(payload));
    expect(adminService.enviarMensajeWhatsapp).toHaveBeenCalledWith(payload);
  });

  it('delega la detección del sender al backend', async () => {
    adminService.detectarChannelSenderWhatsapp.mockReturnValue(of({ detectado: true }));
    await firstValueFrom(facade.detectarChannelSender());
    expect(adminService.detectarChannelSenderWhatsapp).toHaveBeenCalledOnce();
  });
});
