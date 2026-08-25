import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { CajaService } from '../../../core/caja/caja.service';
import { CajaMovementsFacade } from './caja-movements.facade';

describe('CajaMovementsFacade', () => {
  it('delega el movimiento normalizado al servicio HTTP', () => {
    const cajaService = { registrarMovimiento: vi.fn().mockReturnValue(of({})) };
    TestBed.configureTestingModule({ providers: [CajaMovementsFacade, { provide: CajaService, useValue: cajaService }] });
    const facade = TestBed.inject(CajaMovementsFacade);
    const payload = { sucursalId: 4, tipoMovimiento: 'CATALOGO', monto: 80, metodoPago: null, concepto: 'Compra', referencia: null, observaciones: null };

    facade.register(payload).subscribe();

    expect(cajaService.registrarMovimiento).toHaveBeenCalledWith(payload);
  });
});
