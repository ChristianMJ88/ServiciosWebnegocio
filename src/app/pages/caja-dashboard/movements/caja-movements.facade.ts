import { Injectable, inject } from '@angular/core';
import { CajaService, RegistrarMovimientoCajaPayload } from '../../../core/caja/caja.service';

@Injectable({ providedIn: 'root' })
export class CajaMovementsFacade {
  private readonly cajaService = inject(CajaService);

  register(payload: RegistrarMovimientoCajaPayload) {
    return this.cajaService.registrarMovimiento(payload);
  }
}
