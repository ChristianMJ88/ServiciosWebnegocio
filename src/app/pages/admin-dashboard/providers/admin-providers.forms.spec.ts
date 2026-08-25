import { describe, expect, it } from 'vitest';
import { PrestadorAdmin, ServicioAdmin } from '../../../core/admin/admin.service';
import {
  alternarServicioPrestador,
  construirPayloadPrestador,
  crearFormularioPrestador,
  filtrarServiciosPrestador,
  sincronizarServiciosPrestador
} from './admin-providers.forms';

describe('admin providers forms', () => {
  it('crea y edita formularios sin compartir colecciones', () => {
    const prestador = {
      sucursalId: 2,
      correo: 'ANA@EXAMPLE.COM',
      nombreMostrar: 'Ana',
      biografia: null,
      colorAgenda: null,
      activo: true,
      servicioIds: [4]
    } as PrestadorAdmin;
    const formulario = crearFormularioPrestador(0, prestador);

    formulario.servicioIds.push(9);
    expect(prestador.servicioIds).toEqual([4]);
    expect(formulario).toEqual(expect.objectContaining({ sucursalId: 2, contrasenaTemporal: '' }));
  });

  it('normaliza el payload antes de enviarlo', () => {
    const payload = construirPayloadPrestador({
      ...crearFormularioPrestador(1),
      correo: ' ANA@EXAMPLE.COM ',
      contrasenaTemporal: '  ',
      biografia: ' Perfil ',
      colorAgenda: ''
    });

    expect(payload).toEqual(expect.objectContaining({
      correo: 'ana@example.com',
      contrasenaTemporal: null,
      biografia: 'Perfil',
      colorAgenda: null
    }));
  });

  it('filtra y sincroniza servicios por sucursal', () => {
    const servicios = [
      { id: 1, sucursalId: 1, sucursalIds: [1] },
      { id: 2, sucursalId: 2, sucursalIds: [1, 2] },
      { id: 3, sucursalId: 2, sucursalIds: [2] }
    ] as ServicioAdmin[];
    const disponibles = filtrarServiciosPrestador(servicios, 1);

    expect(disponibles.map(servicio => servicio.id)).toEqual([1, 2]);
    expect(sincronizarServiciosPrestador([2, 3], disponibles)).toEqual([2]);
  });

  it('alterna servicios sin mutar la selección original', () => {
    const originales = [1];
    expect(alternarServicioPrestador(originales, 2, true)).toEqual([1, 2]);
    expect(alternarServicioPrestador(originales, 1, false)).toEqual([]);
    expect(originales).toEqual([1]);
  });
});
