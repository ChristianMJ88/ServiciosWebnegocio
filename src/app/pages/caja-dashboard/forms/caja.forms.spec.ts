import { describe, expect, it } from 'vitest';
import {
  construirApertura,
  construirMovimiento,
  construirPago,
  crearFormularioApertura,
  crearFormularioMovimiento,
  crearFormularioPago
} from './caja.forms';

describe('caja forms', () => {
  it('crea formularios sin catálogos locales', () => {
    expect(crearFormularioPago().metodoPago).toBe('');
    expect(crearFormularioMovimiento()).toEqual(expect.objectContaining({ tipoMovimiento: '', metodoPago: '' }));
  });

  it('normaliza apertura y pago', () => {
    expect(construirApertura(3, { ...crearFormularioApertura(), montoInicial: 100, observaciones: '  ' }))
      .toEqual({ sucursalId: 3, montoInicial: 100, observaciones: null });
    expect(construirPago({ ...crearFormularioPago(), monto: 25, metodoPago: 'TARJETA', referencia: ' A1 ' }))
      .toEqual(expect.objectContaining({ monto: 25, metodoPago: 'TARJETA', referencia: 'A1' }));
  });

  it('normaliza el movimiento sin decidir sus códigos', () => {
    expect(construirMovimiento(2, {
      ...crearFormularioMovimiento('SALIDA', 'EFECTIVO'),
      monto: 10,
      concepto: ' Insumo '
    })).toEqual(expect.objectContaining({ sucursalId: 2, tipoMovimiento: 'SALIDA', concepto: 'Insumo' }));
  });
});
