import { describe, expect, it } from 'vitest';
import { RolInternoAdmin } from '../../core/admin/admin.service';
import {
  cambiarPermiso,
  construirPayloadUsuarioInterno,
  crearFormularioRolClonado,
  crearFormularioUsuarioInterno,
  quitarPermisosHeredados
} from './admin-access.helpers';

describe('admin access helpers', () => {
  it('normaliza datos sin decidir autorización', () => {
    const formulario = crearFormularioUsuarioInterno(3, 7);
    const payload = construirPayloadUsuarioInterno({
      ...formulario,
      correo: ' ADMIN@EXAMPLE.COM ',
      nombreCompleto: ' Administrador ',
      telefono: ' ',
      permisosDirectos: ['B', 'A', 'B']
    });

    expect(payload.correo).toBe('admin@example.com');
    expect(payload.nombreCompleto).toBe('Administrador');
    expect(payload.telefono).toBeNull();
    expect(payload.permisosDirectos).toEqual(['A', 'B']);
  });

  it('separa permisos directos de los heredados por el rol', () => {
    const rol = { permisos: ['AGENDA_VER', 'CAJA_VER'] } as RolInternoAdmin;
    expect(quitarPermisosHeredados(['EXTRA', 'CAJA_VER'], rol)).toEqual(['EXTRA']);
    expect(cambiarPermiso(['EXTRA'], 'NUEVO', true)).toEqual(['EXTRA', 'NUEVO']);
  });

  it('genera identificadores únicos al clonar solamente para presentación', () => {
    const crearRol = (id: number, codigo: string, nombre: string): RolInternoAdmin => ({
      id,
      codigo,
      nombre,
      descripcion: null,
      activo: true,
      editable: true,
      usuariosAsignados: 0,
      sePuedeEliminar: true,
      permisos: []
    });
    const origen = crearRol(1, 'CAJA', 'Caja');
    const existentes = [
      origen,
      crearRol(2, 'CAJA_COPIA', 'Caja copia'),
      crearRol(3, 'CAJA_COPIA_2', 'Caja copia 2')
    ];

    const formulario = crearFormularioRolClonado(origen, existentes);
    expect(formulario.codigo).toBe('CAJA_COPIA_3');
    expect(formulario.nombre).toBe('Caja copia 3');
  });
});
