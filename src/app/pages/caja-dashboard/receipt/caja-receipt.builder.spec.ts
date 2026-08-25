import { describe, expect, it } from 'vitest';
import { CitaPorCobrar, PagoCita } from '../../../core/caja/caja.service';
import { construirComprobanteCaja } from './caja-receipt.builder';

describe('construirComprobanteCaja', () => {
  it('calcula totales y usa etiquetas del catálogo backend', () => {
    const cita = { clienteNombre: 'Ana', sucursalNombre: 'Centro', servicioNombre: 'Consulta', inicio: '2026-08-25T10:00:00', moneda: 'MXN', total: 500, pagado: 200, pendiente: 300 } as CitaPorCobrar;
    const pagos = [{ monto: 200, metodoPago: 'TARJETA', referencia: null, registradoEn: '2026-08-25T10:30:00' }] as PagoCita[];

    const recibo = construirComprobanteCaja('Empresa', cita, pagos, [{ codigo: 'TARJETA', etiqueta: 'Tarjeta bancaria' }]);

    expect(recibo.totalPagado).toBe(200);
    expect(recibo.pendiente).toBe(300);
    expect(recibo.pagos[0]).toMatchObject({ metodo: 'Tarjeta bancaria', referencia: '-' });
  });

  it('respeta los totales calculados por el backend', () => {
    const cita = { total: 100, pagado: 120, pendiente: 0, moneda: 'MXN' } as CitaPorCobrar;
    const pagos = [{ monto: 80, metodoPago: 'EFECTIVO' }] as PagoCita[];

    expect(construirComprobanteCaja('Empresa', cita, pagos, [])).toMatchObject({ totalPagado: 120, pendiente: 0 });
  });
});
