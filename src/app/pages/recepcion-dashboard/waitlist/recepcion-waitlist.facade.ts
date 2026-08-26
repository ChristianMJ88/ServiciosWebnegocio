import { Injectable, inject } from '@angular/core';
import { RecepcionService } from '../../../core/recepcion/recepcion.service';

@Injectable({ providedIn: 'root' })
export class RecepcionWaitlistFacade {
  private readonly recepcionService = inject(RecepcionService);

  notificar(solicitudId: number) {
    return this.recepcionService.notificarEspera(solicitudId);
  }
}
