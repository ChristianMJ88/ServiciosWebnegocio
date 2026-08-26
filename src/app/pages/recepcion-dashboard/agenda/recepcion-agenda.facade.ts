import { Injectable, inject } from '@angular/core';
import { RecepcionService } from '../../../core/recepcion/recepcion.service';

@Injectable({ providedIn: 'root' })
export class RecepcionAgendaFacade {
  private readonly recepcionService = inject(RecepcionService);

  hacerCheckIn(citaId: number) {
    return this.recepcionService.checkIn(citaId);
  }

  confirmar(citaId: number) {
    return this.recepcionService.confirmar(citaId);
  }

  cancelar(citaId: number) {
    return this.recepcionService.cancelar(citaId);
  }

  finalizar(citaId: number) {
    return this.recepcionService.finalizar(citaId);
  }
}
