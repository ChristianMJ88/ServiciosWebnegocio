import { describe, expect, it } from 'vitest';
import { PrestadorAdmin, SucursalAdmin } from '../../core/admin/admin.service';
import {
  construirPayloadExcepcion,
  construirPayloadRegla,
  construirSujetosDisponibilidad,
  crearFormularioExcepcion,
  crearFormularioRegla,
  sincronizarSujetoSeleccionado
} from './admin-availability.forms';

describe('admin availability forms', () => {
  it('crea formularios vacíos sin imponer reglas de negocio', () => {
    expect(crearFormularioRegla()).toEqual(expect.objectContaining({
      tipoSujeto: '',
      sujetoId: 0,
      horaInicio: '',
      horaFin: '',
      intervaloMinutos: 0
    }));
    expect(crearFormularioExcepcion()).toEqual(expect.objectContaining({
      tipoSujeto: '',
      sujetoId: 0,
      fechaExcepcion: '',
      tipoBloqueo: ''
    }));
  });

  it('normaliza valores opcionales antes de enviarlos', () => {
    expect(construirPayloadRegla({
      ...crearFormularioRegla(),
      vigenteDesde: '   ',
      vigenteHasta: '2026-09-01'
    })).toEqual(expect.objectContaining({ vigenteDesde: null, vigenteHasta: '2026-09-01' }));

    expect(construirPayloadExcepcion({
      ...crearFormularioExcepcion(),
      horaInicio: '',
      horaFin: '  ',
      motivo: ' Vacaciones '
    })).toEqual(expect.objectContaining({ horaInicio: null, horaFin: null, motivo: 'Vacaciones' }));
  });

  it('construye opciones según el tipo y conserva solo una selección válida', () => {
    const sucursales = [{ id: 3, nombre: 'Centro' }] as SucursalAdmin[];
    const prestadores = [{ usuarioId: 8, nombreMostrar: 'Ana', sucursalNombre: 'Norte' }] as PrestadorAdmin[];

    expect(construirSujetosDisponibilidad('SUCURSAL', sucursales, prestadores))
      .toEqual([{ id: 3, nombre: 'Centro' }]);
    expect(construirSujetosDisponibilidad('PRESTADOR', sucursales, prestadores))
      .toEqual([{ id: 8, nombre: 'Ana · Norte' }]);
    expect(sincronizarSujetoSeleccionado(99, [{ id: 3, nombre: 'Centro' }])).toBe(3);
    expect(sincronizarSujetoSeleccionado(3, [{ id: 3, nombre: 'Centro' }])).toBe(3);
  });
});
