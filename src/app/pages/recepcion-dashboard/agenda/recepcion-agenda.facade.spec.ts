import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { RecepcionService } from '../../../core/recepcion/recepcion.service';
import { RecepcionAgendaFacade } from './recepcion-agenda.facade';

describe('RecepcionAgendaFacade', () => {
  it.each([
    ['hacerCheckIn', 'checkIn'],
    ['confirmar', 'confirmar'],
    ['cancelar', 'cancelar'],
    ['finalizar', 'finalizar']
  ] as const)('delega %s al endpoint correspondiente', async (metodoFacade, metodoServicio) => {
    const recepcionService = {
      checkIn: vi.fn().mockReturnValue(of({ id: 18 })),
      confirmar: vi.fn().mockReturnValue(of({ id: 18 })),
      cancelar: vi.fn().mockReturnValue(of({ id: 18 })),
      finalizar: vi.fn().mockReturnValue(of({ id: 18 }))
    };
    TestBed.configureTestingModule({
      providers: [RecepcionAgendaFacade, { provide: RecepcionService, useValue: recepcionService }]
    });

    await firstValueFrom(TestBed.inject(RecepcionAgendaFacade)[metodoFacade](18));

    expect(recepcionService[metodoServicio]).toHaveBeenCalledWith(18);
  });
});
