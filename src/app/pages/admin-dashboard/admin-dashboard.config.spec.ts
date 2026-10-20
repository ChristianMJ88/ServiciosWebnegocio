import { describe, expect, it } from 'vitest';
import { PERMISOS } from '../../core/auth/permissions';
import { construirGruposSidebarAdmin, esSeccionAdmin, filtrarModulosAdmin, filtrarPanelesOperativosAdmin, seleccionarModuloActivo } from './admin-dashboard.config';

describe('configuración del menú administrativo', () => {
  it('admite una lista vacía de módulos concedidos por backend', () => {
    const modulos = filtrarModulosAdmin([], new Set());
    expect(modulos).toEqual([]);
    expect(seleccionarModuloActivo(modulos, 'resumen')).toBeNull();
  });

  it('elige el primer módulo permitido si la sección solicitada no está disponible', () => {
    const modulos = filtrarModulosAdmin([PERMISOS.serviciosGestionar], new Set(['servicios']));
    expect(modulos.length).toBeGreaterThan(0);
    expect(seleccionarModuloActivo(modulos, 'pagos')).toBe(modulos[0]);
  });

  it('conserva la sección solicitada cuando pertenece a los módulos permitidos', () => {
    const modulos = filtrarModulosAdmin([PERMISOS.serviciosGestionar], new Set(['resumen', 'servicios']));
    const seleccionado = seleccionarModuloActivo(modulos, 'servicios');
    expect(seleccionado).toBe(modulos.find(modulo => modulo.id === 'servicios'));
    expect(seleccionado?.id).toBe('servicios');
  });
  it('permite abrir Caja y Recepción cuando capacidad y permiso son concedidos por el backend', () => {
    expect(filtrarPanelesOperativosAdmin([PERMISOS.cajaAcceso, PERMISOS.recepcionAcceso], new Set(['caja', 'recepcion']))
      .map(panel => panel.ruta)).toEqual(['/recepcion', '/caja']);
  });

  it('no muestra paneles que el plan o el usuario no pueden abrir', () => {
    expect(filtrarPanelesOperativosAdmin([PERMISOS.cajaAcceso], new Set(['recepcion']))).toEqual([]);
    expect(filtrarPanelesOperativosAdmin([], new Set(['caja', 'recepcion']))).toEqual([]);
  });
  it('muestra únicamente módulos autorizados por los permisos del backend', () => {
    const modulos = filtrarModulosAdmin([PERMISOS.serviciosGestionar]);

    expect(modulos.map(modulo => modulo.id)).toEqual(['resumen', 'servicios']);
  });

  it('omite grupos que no contienen módulos autorizados', () => {
    const grupos = construirGruposSidebarAdmin(filtrarModulosAdmin([]));

    expect(grupos.map(grupo => grupo.id)).toEqual(['operacion']);
    expect(grupos[0].modulosVisibles.map(modulo => modulo.id)).toEqual(['resumen']);
  });

  it('valida las secciones usando la configuración central', () => {
    expect(esSeccionAdmin('contactos')).toBe(true);
    expect(esSeccionAdmin('whatsapp')).toBe(true);
    expect(esSeccionAdmin('empresa-especifica')).toBe(false);
  });

  it('ubica envíos en logística cuando el backend concede el permiso', () => {
    const grupos = construirGruposSidebarAdmin(filtrarModulosAdmin([PERMISOS.enviosConfigurar]));

    expect(grupos.find(grupo => grupo.id === 'logistica')?.modulosVisibles.map(modulo => modulo.id))
      .toEqual(['envios']);
  });

  it('muestra pagos en comercial únicamente cuando el backend concede el permiso', () => {
    const grupos = construirGruposSidebarAdmin(filtrarModulosAdmin([PERMISOS.pagosConfigurar]));

    expect(grupos.find(grupo => grupo.id === 'comercial')?.modulosVisibles.map(modulo => modulo.id))
      .toEqual(['pagos', 'liquidaciones']);
  });

  it('combina permisos del usuario con las secciones autorizadas por el backend', () => {
    const permisos = [PERMISOS.productosGestionar, PERMISOS.inventarioGestionar, PERMISOS.pagosConfigurar];
    const modulos = filtrarModulosAdmin(permisos, new Set(['productos']));

    expect(modulos.map(modulo => modulo.id)).toContain('productos');
    expect(modulos.map(modulo => modulo.id)).not.toContain('inventario');
    expect(modulos.map(modulo => modulo.id)).not.toContain('pagos');
  });
});
