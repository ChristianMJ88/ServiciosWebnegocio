import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { RecepcionDashboardStore } from './recepcion-dashboard.store';

describe('RecepcionDashboardStore', () => {
  it('publica estado de solo lectura y actualiza el snapshot operativo', () => {
    TestBed.configureTestingModule({ providers: [RecepcionDashboardStore] });
    const store = TestBed.inject(RecepcionDashboardStore);

    store.setSucursalActivaId(12);
    store.setCitas([{ id: 4 }] as never[]);
    store.setSolicitudesEspera([{ id: 7 }] as never[]);

    expect(store.sucursalActivaId()).toBe(12);
    expect(store.citas()).toEqual([{ id: 4 }]);
    expect(store.solicitudesEspera()).toEqual([{ id: 7 }]);
  });

  it('actualiza solicitudes sin exponer la señal mutable', () => {
    TestBed.configureTestingModule({ providers: [RecepcionDashboardStore] });
    const store = TestBed.inject(RecepcionDashboardStore);
    store.setSolicitudesEspera([{ id: 1 }] as never[]);

    store.updateSolicitudesEspera(actuales => [{ id: 2 } as never, ...actuales]);

    expect(store.solicitudesEspera().map(item => item.id)).toEqual([2, 1]);
  });
});
