import { describe, expect, it } from 'vitest';
import { SolicitudContactoAdmin } from '../../../core/admin/admin.service';
import { reemplazarContacto, resumirContactos } from './admin-contacts.helpers';

const contacto = (id: number, estado: string) => ({ id, estado }) as SolicitudContactoAdmin;

describe('admin contacts helpers', () => {
  it('resume usando los códigos entregados por el backend', () => {
    const metadatos = {
      estados: [],
      estadoNuevo: 'PENDIENTE',
      estadoEnProceso: 'TRABAJANDO',
      estadoAtendido: 'RESUELTO'
    };
    expect(resumirContactos([
      contacto(1, 'PENDIENTE'),
      contacto(2, 'TRABAJANDO'),
      contacto(3, 'RESUELTO')
    ], metadatos)).toEqual({ total: 3, nuevos: 1, enProceso: 1, atendidos: 1 });
  });

  it('reemplaza sólo el contacto actualizado', () => {
    const actualizado = contacto(2, 'RESUELTO');
    expect(reemplazarContacto([contacto(1, 'PENDIENTE'), contacto(2, 'TRABAJANDO')], actualizado))
      .toEqual([contacto(1, 'PENDIENTE'), actualizado]);
  });
});
