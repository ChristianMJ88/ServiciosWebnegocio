import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { CajaDashboardCoordinator } from './caja-dashboard.coordinator';
import { CajaDashboardFacade } from './caja-dashboard.facade';
import { CajaDashboardStore } from '../state/caja-dashboard.store';

describe('CajaDashboardCoordinator', () => {
  it('aplica catálogo y snapshot al store', async () => {
    const catalogo = { sucursalActivaId: 7, sucursales: [], metodosPago: [], tiposMovimiento: [], estadosSesion: [], estadoSesionAbierta: '', estadoSesionCerrada: '', metodoPagoEfectivo: '' };
    const snapshot = { sesion: null, citas: [], resumen: { saldoEsperadoCaja: 40 }, movimientos: [] };
    const facade = { cargarConCatalogo: vi.fn().mockReturnValue(of({ catalogo, tablero: snapshot })), cargarTablero: vi.fn() };
    TestBed.configureTestingModule({ providers: [CajaDashboardCoordinator, CajaDashboardStore, { provide: CajaDashboardFacade, useValue: facade }] });
    const store = TestBed.inject(CajaDashboardStore);

    await firstValueFrom(TestBed.inject(CajaDashboardCoordinator).load(2));

    expect(store.resumen()?.saldoEsperadoCaja).toBe(40);
    expect(store.loading()).toBe(false);
  });

  it('normaliza el error y finaliza la carga', () => {
    const facade = { cargarConCatalogo: vi.fn().mockReturnValue(throwError(() => ({ error: { message: 'Backend no disponible' } }))), cargarTablero: vi.fn() };
    TestBed.configureTestingModule({ providers: [CajaDashboardCoordinator, CajaDashboardStore, { provide: CajaDashboardFacade, useValue: facade }] });
    const store = TestBed.inject(CajaDashboardStore);

    TestBed.inject(CajaDashboardCoordinator).load().subscribe();

    expect(store.error()).toBe('Backend no disponible');
    expect(store.loading()).toBe(false);
  });
});
