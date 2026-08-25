import { Injectable, inject } from '@angular/core';
import { CajaService, RegistrarPagoPayload } from '../../../core/caja/caja.service';

@Injectable({ providedIn: 'root' })
export class CajaPaymentsFacade {
  private readonly cajaService = inject(CajaService);

  list(appointmentId: number) {
    return this.cajaService.listarPagosCita(appointmentId);
  }

  register(appointmentId: number, payload: RegistrarPagoPayload) {
    return this.cajaService.registrarPago(appointmentId, payload);
  }
}
