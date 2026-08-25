import { describe, expect, it } from 'vitest';
import { GuardarServicioPayload } from '../../../core/admin/admin.service';
import {
  construirPayloadGrupoServicio,
  construirPayloadServicio,
  construirPayloadSucursal,
  crearFormularioServicio,
  mergeCatalogoById,
  ordenarCatalogo
} from './admin-catalog.helpers';

describe('admin catalog helpers', () => {
  it('normaliza campos opcionales antes de guardar', () => {
    expect(construirPayloadSucursal({
      nombre: 'Centro',
      direccion: '   ',
      telefono: ' 555 123 ',
      zonaHoraria: 'America/Mexico_City',
      activa: true
    })).toMatchObject({ direccion: null, telefono: '555 123' });

    expect(construirPayloadGrupoServicio({
      nombre: 'Uñas',
      slug: ' unas ',
      descripcion: '',
      imagenUrl: ' ',
      icono: ' sparkle ',
      ordenPublico: 10,
      activo: true
    })).toMatchObject({ slug: 'unas', descripcion: null, imagenUrl: null, icono: 'sparkle' });
  });

  it('elimina datos de anticipo cuando el servicio no lo requiere', () => {
    const formulario = crearFormularioServicio(7);
    const payload = construirPayloadServicio({
      ...formulario,
      nombre: 'Manicure',
      moneda: 'mxn',
      requiereAnticipo: false,
      anticipoTipo: 'PORCENTAJE',
      anticipoValor: 25
    } as GuardarServicioPayload);

    expect(payload.sucursalIds).toEqual([7]);
    expect(payload.moneda).toBe('MXN');
    expect(payload.anticipoTipo).toBeNull();
    expect(payload.anticipoValor).toBeNull();
  });

  it('combina cambios por id y conserva un orden estable', () => {
    const actuales = [
      { id: 1, nombre: 'B', ordenPublico: 20 },
      { id: 2, nombre: 'A', ordenPublico: 10 }
    ];
    const cambios = [{ id: 1, nombre: 'C', ordenPublico: 5 }];

    expect(mergeCatalogoById(actuales, cambios).sort(ordenarCatalogo).map(item => item.id))
      .toEqual([1, 2]);
  });
});
