import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { CajaService } from '../../../core/caja/caja.service';
import { CajaSessionFacade } from './caja-session.facade';

describe('CajaSessionFacade', () => {
  it('delega apertura y cierre usando DTOs explícitos', () => {
    const cajaService = { abrirCaja: vi.fn().mockReturnValue(of({})), cerrarCaja: vi.fn().mockReturnValue(of({})) };
    TestBed.configureTestingModule({ providers: [CajaSessionFacade, { provide: CajaService, useValue: cajaService }] });
    const facade = TestBed.inject(CajaSessionFacade);
    const apertura = { sucursalId: 7, montoInicial: 100, observaciones: null };
    const cierre = { montoContado: 250, observaciones: null };

    facade.open(apertura).subscribe();
    facade.close(3, cierre).subscribe();

    expect(cajaService.abrirCaja).toHaveBeenCalledWith(apertura);
    expect(cajaService.cerrarCaja).toHaveBeenCalledWith(3, cierre);
  });
});
