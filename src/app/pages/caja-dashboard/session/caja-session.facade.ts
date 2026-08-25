import { Injectable, inject } from '@angular/core';
import { AbrirCajaPayload, CajaService, CerrarCajaPayload } from '../../../core/caja/caja.service';

@Injectable({ providedIn: 'root' })
export class CajaSessionFacade {
  private readonly cajaService = inject(CajaService);

  open(payload: AbrirCajaPayload) {
    return this.cajaService.abrirCaja(payload);
  }

  close(sessionId: number, payload: CerrarCajaPayload) {
    return this.cajaService.cerrarCaja(sessionId, payload);
  }
}
