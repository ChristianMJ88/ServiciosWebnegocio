import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { RecepcionService } from '../../../core/recepcion/recepcion.service';
import { RecepcionDashboardFacade } from './recepcion-dashboard.facade';

describe('RecepcionDashboardFacade', () => {
  it('carga agenda y espera con la sucursal resuelta por el catálogo del backend', async () => {
    const recepcionService = {
      getCatalogo: vi.fn().mockReturnValue(of({ sucursalActivaId: 8, sucursales: [], servicios: [] })),
      getAgenda: vi.fn().mockReturnValue(of([])),
      getSolicitudesEspera: vi.fn().mockReturnValue(of([]))
    };
    TestBed.configureTestingModule({
      providers: [RecepcionDashboardFacade, { provide: RecepcionService, useValue: recepcionService }]
    });

    const resultado = await firstValueFrom(
      TestBed.inject(RecepcionDashboardFacade).cargarConCatalogo('2026-08-25', 3)
    );

    expect(recepcionService.getAgenda).toHaveBeenCalledWith('2026-08-25', 8);
    expect(recepcionService.getSolicitudesEspera).toHaveBeenCalledWith('2026-08-25', 8);
    expect(resultado.catalogo.sucursalActivaId).toBe(8);
  });

  it('mantiene disponible la agenda cuando falla la lista de espera', async () => {
    const recepcionService = {
      getCatalogo: vi.fn().mockReturnValue(of({ sucursalActivaId: 8, sucursales: [], servicios: [] })),
      getAgenda: vi.fn().mockReturnValue(of([{ id: 21 }])),
      getSolicitudesEspera: vi.fn().mockReturnValue(throwError(() => new Error('fallo')))
    };
    TestBed.configureTestingModule({
      providers: [RecepcionDashboardFacade, { provide: RecepcionService, useValue: recepcionService }]
    });

    const resultado = await firstValueFrom(
      TestBed.inject(RecepcionDashboardFacade).cargarConCatalogo('2026-08-25', 8)
    );

    expect(resultado.agenda).toEqual([{ id: 21 }]);
    expect(resultado.espera).toEqual([]);
  });
});
