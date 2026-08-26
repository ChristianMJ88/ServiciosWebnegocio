import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { RecepcionDashboardStore } from '../state/recepcion-dashboard.store';
import { RecepcionDashboardCoordinator } from './recepcion-dashboard.coordinator';
import { RecepcionDashboardFacade } from './recepcion-dashboard.facade';

describe('RecepcionDashboardCoordinator', () => {
  it('aplica catálogo y snapshot al store', async () => {
    const catalogo = {
      sucursalActivaId: 5,
      sucursales: [{ id: 5 }],
      servicios: [],
      estadosCita: [],
      estadoCitaPendiente: 'PENDIENTE',
      estadoCitaConfirmada: 'CONFIRMADA',
      estadosCitaFinalizables: [],
      estadosCitaCancelables: [],
      estadosEspera: [],
      estadoEsperaPendiente: '',
      estadoEsperaNotificada: ''
    };
    const facade = {
      cargarConCatalogo: vi.fn().mockReturnValue(of({ catalogo, agenda: [{ id: 9 }], espera: [] })),
      cargarSnapshot: vi.fn()
    };
    TestBed.configureTestingModule({
      providers: [RecepcionDashboardCoordinator, RecepcionDashboardStore, { provide: RecepcionDashboardFacade, useValue: facade }]
    });
    const store = TestBed.inject(RecepcionDashboardStore);

    await firstValueFrom(TestBed.inject(RecepcionDashboardCoordinator).load('2026-08-25', 2));

    expect(store.citas()).toEqual([{ id: 9 }]);
    expect(store.estadoCitaPendiente()).toBe('PENDIENTE');
    expect(store.loading()).toBe(false);
  });

  it('normaliza errores y finaliza la carga', () => {
    const facade = {
      cargarConCatalogo: vi.fn().mockReturnValue(throwError(() => ({ error: { message: 'Backend no disponible' } }))),
      cargarSnapshot: vi.fn()
    };
    TestBed.configureTestingModule({
      providers: [RecepcionDashboardCoordinator, RecepcionDashboardStore, { provide: RecepcionDashboardFacade, useValue: facade }]
    });
    const store = TestBed.inject(RecepcionDashboardStore);

    TestBed.inject(RecepcionDashboardCoordinator).load('2026-08-25').subscribe();

    expect(store.error()).toBe('Backend no disponible');
    expect(store.loading()).toBe(false);
  });
});
