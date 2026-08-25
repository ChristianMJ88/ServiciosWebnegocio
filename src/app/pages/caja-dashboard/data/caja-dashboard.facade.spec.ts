import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { CajaService } from '../../../core/caja/caja.service';
import { CajaDashboardFacade } from './caja-dashboard.facade';

describe('CajaDashboardFacade', () => {
  it('combina catálogo y tablero usando la sucursal resuelta por backend', async () => {
    const cajaService = {
      getCatalogo: vi.fn().mockReturnValue(of({ sucursalActivaId: 7, sucursales: [] })),
      getSesionActual: vi.fn().mockReturnValue(of(null)),
      getCitasPorCobrar: vi.fn().mockReturnValue(of([])),
      getResumen: vi.fn().mockReturnValue(of({ saldoEsperadoCaja: 0 })),
      listarMovimientos: vi.fn().mockReturnValue(of([]))
    };
    TestBed.configureTestingModule({
      providers: [CajaDashboardFacade, { provide: CajaService, useValue: cajaService }]
    });
    const resultado = await firstValueFrom(TestBed.inject(CajaDashboardFacade).cargarConCatalogo(2));
    expect(cajaService.getSesionActual).toHaveBeenCalledWith(7);
    expect(resultado.catalogo.sucursalActivaId).toBe(7);
  });
});
