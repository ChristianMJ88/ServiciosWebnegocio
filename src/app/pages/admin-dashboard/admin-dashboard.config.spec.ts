import { describe, expect, it } from 'vitest';
import { PERMISOS } from '../../core/auth/permissions';
import { construirGruposSidebarAdmin, filtrarModulosAdmin } from './admin-dashboard.config';

describe('configuración del menú administrativo', () => {
  it('muestra únicamente módulos autorizados por los permisos del backend', () => {
    const modulos = filtrarModulosAdmin([PERMISOS.serviciosGestionar]);

    expect(modulos.map(modulo => modulo.id)).toEqual(['resumen', 'servicios']);
  });

  it('omite grupos que no contienen módulos autorizados', () => {
    const grupos = construirGruposSidebarAdmin(filtrarModulosAdmin([]));

    expect(grupos.map(grupo => grupo.id)).toEqual(['operacion']);
    expect(grupos[0].modulosVisibles.map(modulo => modulo.id)).toEqual(['resumen']);
  });
});
