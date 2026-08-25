import { describe, expect, it } from 'vitest';
import { CajaSesion, CitaPorCobrar, ResumenCaja } from '../../../core/caja/caja.service';
import { CajaDashboardStore } from './caja-dashboard.store';

describe('CajaDashboardStore', () => {
  it('deriva estado y etiquetas desde el catálogo backend', () => {
    const store = new CajaDashboardStore();
    store.applyCatalog({ sucursalActivaId: 1, sucursales: [], metodosPago: [], tiposMovimiento: [], estadosSesion: [{ codigo: 'OPEN', etiqueta: 'Operando' }], estadoSesionAbierta: 'OPEN', estadoSesionCerrada: 'CLOSED', metodoPagoEfectivo: '' });
    store.setSession({ estado: 'OPEN' } as CajaSesion);

    expect(store.cajaAbierta()).toBe(true);
    expect(store.estadoSesionEtiqueta()).toBe('Operando');
  });

  it('aplica el snapshot y calcula métricas sin mutar las respuestas', () => {
    const store = new CajaDashboardStore();
    const citas = [{ citaId: 1, pendiente: 120 }, { citaId: 2, pendiente: 80 }] as CitaPorCobrar[];
    store.applyDashboard({ sesion: null, citas, resumen: { totalCobrado: 50, saldoEsperadoCaja: 30 } as ResumenCaja, movimientos: [] });

    expect(store.totalPendiente()).toBe(200);
    expect(store.metricas().find(metrica => metrica.label === 'Citas por cobrar')?.value).toBe(2);
    expect(citas).toHaveLength(2);
  });
});
