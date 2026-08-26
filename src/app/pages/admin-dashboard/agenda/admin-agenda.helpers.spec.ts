import { describe, expect, it } from 'vitest';
import { CitaCliente } from '../../../core/auth/client-appointments.service';
import {
  calcularAnaliticaAgenda,
  calcularDistribucionAgenda,
  filtrarCitasPeriodo,
  obtenerRangoMes,
  obtenerRangoSemana,
  resumirAgenda,
  sumarDiasAgenda,
  sumarMesesAgenda
} from './admin-agenda.helpers';

const cita = (id: number, inicio: string, fin: string, estado = 'PENDIENTE', prestadorId = 1): CitaCliente => ({
  id,
  estado,
  sucursalId: 1,
  servicioId: 1,
  prestadorId,
  sucursalNombre: 'Centro',
  servicioNombre: 'Servicio',
  prestadorNombre: `Prestador ${prestadorId}`,
  inicio,
  fin,
  precio: 100,
  moneda: 'MXN',
  notas: null,
  cancelable: true
});

describe('admin agenda helpers', () => {
  it('calcula semanas de lunes a domingo y desplazamientos sin depender del componente', () => {
    expect(obtenerRangoSemana('2026-08-25')).toEqual({ desde: '2026-08-24', hasta: '2026-08-30' });
    expect(sumarDiasAgenda('2026-08-25', 7)).toBe('2026-09-01');
  });

  it('calcula rangos y desplazamientos mensuales preservando un día válido', () => {
    expect(obtenerRangoMes('2026-08-25')).toEqual({ desde: '2026-08-01', hasta: '2026-08-31' });
    expect(sumarMesesAgenda('2026-01-31', 1)).toBe('2026-02-28');
  });

  it('filtra el periodo y resume estados y colaboradores', () => {
    const citas = [
      cita(1, '2026-08-24T09:00:00', '2026-08-24T10:00:00'),
      cita(2, '2026-08-30T11:00:00', '2026-08-30T12:00:00', 'CONFIRMADA', 2),
      cita(3, '2026-09-01T11:00:00', '2026-09-01T12:00:00')
    ];
    const periodo = filtrarCitasPeriodo(citas, '2026-08-25', 'week');
    expect(periodo.map(item => item.id)).toEqual([1, 2]);
    expect(resumirAgenda(periodo)).toEqual({ total: 2, pendientes: 1, confirmadas: 1, colaboradores: 2 });
  });

  it('calcula ocupación usando la capacidad recibida', () => {
    const resultado = calcularAnaliticaAgenda(
      [cita(1, '2026-08-25T09:00:00', '2026-08-25T10:00:00')],
      1,
      8,
      'day'
    );
    expect(resultado).toEqual({ ocupacion: 13, horasReservadas: 1, horasLibres: 7 });
  });

  it('asigna carriles distintos a citas superpuestas', () => {
    const primera = cita(1, '2026-08-25T09:00:00', '2026-08-25T10:00:00');
    const segunda = cita(2, '2026-08-25T09:30:00', '2026-08-25T10:30:00');
    const distribucion = calcularDistribucionAgenda([primera, segunda]);
    expect(distribucion.get(primera)).toEqual({ lane: 0, laneCount: 2 });
    expect(distribucion.get(segunda)).toEqual({ lane: 1, laneCount: 2 });
  });
});
