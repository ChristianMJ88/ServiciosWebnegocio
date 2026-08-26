import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { RecepcionService } from '../../../core/recepcion/recepcion.service';
import { RecepcionWaitlistFacade } from './recepcion-waitlist.facade';

describe('RecepcionWaitlistFacade', () => {
  it('delega la notificación al contrato de recepción', async () => {
    const solicitud = { id: 17 };
    const recepcionService = {
      notificarEspera: vi.fn().mockReturnValue(of(solicitud))
    };
    TestBed.configureTestingModule({
      providers: [RecepcionWaitlistFacade, { provide: RecepcionService, useValue: recepcionService }]
    });

    const resultado = await firstValueFrom(TestBed.inject(RecepcionWaitlistFacade).notificar(17));

    expect(recepcionService.notificarEspera).toHaveBeenCalledWith(17);
    expect(resultado).toBe(solicitud);
  });
});
