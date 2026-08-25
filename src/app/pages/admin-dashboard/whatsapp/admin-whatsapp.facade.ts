import { Injectable, inject } from '@angular/core';
import {
  AdminService,
  AsociarChannelSenderWhatsappPayload,
  EnviarMensajeWhatsappPayload,
  GuardarConfiguracionWhatsappPayload,
  GuardarPlantillaWhatsappEmpresaPayload,
  ProbarPlantillaWhatsappPayload,
  ProvisionarMessagingServiceWhatsappPayload,
  ProvisionarSubcuentaWhatsappPayload
} from '../../../core/admin/admin.service';

@Injectable({ providedIn: 'root' })
export class AdminWhatsappFacade {
  private readonly adminService = inject(AdminService);

  guardarConfiguracion(payload: GuardarConfiguracionWhatsappPayload) {
    return this.adminService.actualizarConfiguracionWhatsapp(payload);
  }

  provisionarSubcuenta(payload: ProvisionarSubcuentaWhatsappPayload) {
    return this.adminService.provisionarSubcuentaWhatsapp(payload);
  }

  provisionarMessagingService(payload: ProvisionarMessagingServiceWhatsappPayload) {
    return this.adminService.provisionarMessagingServiceWhatsapp(payload);
  }

  asociarChannelSender(payload: AsociarChannelSenderWhatsappPayload) {
    return this.adminService.asociarChannelSenderWhatsapp(payload);
  }

  detectarChannelSender() {
    return this.adminService.detectarChannelSenderWhatsapp();
  }

  probarPlantilla(payload: ProbarPlantillaWhatsappPayload) {
    return this.adminService.probarPlantillaWhatsapp(payload);
  }

  cargarLogs() {
    return this.adminService.getLogsWhatsapp();
  }

  cargarMensajes() {
    return this.adminService.getMensajesWhatsapp();
  }

  enviarMensaje(payload: EnviarMensajeWhatsappPayload) {
    return this.adminService.enviarMensajeWhatsapp(payload);
  }

  guardarPlantilla(id: number | null, payload: GuardarPlantillaWhatsappEmpresaPayload) {
    return id
      ? this.adminService.actualizarPlantillaWhatsappEmpresa(id, payload)
      : this.adminService.crearPlantillaWhatsappEmpresa(payload);
  }

  eliminarPlantilla(id: number) {
    return this.adminService.eliminarPlantillaWhatsappEmpresa(id);
  }
}
