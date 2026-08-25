import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { CajaService } from '../../../core/caja/caja.service';
import { CajaPaymentsFacade } from './caja-payments.facade';

describe('CajaPaymentsFacade', () => {
  it('delega listado y registro de pagos', () => {
    const cajaService = { listarPagosCita: vi.fn().mockReturnValue(of([])), registrarPago: vi.fn().mockReturnValue(of({})) };
    TestBed.configureTestingModule({ providers: [CajaPaymentsFacade, { provide: CajaService, useValue: cajaService }] });
    const facade = TestBed.inject(CajaPaymentsFacade);
    const payload = { monto: 150, metodoPago: 'CATALOGO', referencia: null, observaciones: null };

    facade.list(9).subscribe();
    facade.register(9, payload).subscribe();

    expect(cajaService.listarPagosCita).toHaveBeenCalledWith(9);
    expect(cajaService.registrarPago).toHaveBeenCalledWith(9, payload);
  });
});
