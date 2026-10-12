import { describe, expect, it } from 'vitest';
import { CatalogoInventario, ExistenciaProducto } from './inventory-admin.service';

describe('Lógica y filtros del módulo de inventario', () => {
  const catalogoMock: CatalogoInventario = {
    sucursales: [
      { id: 1, nombre: 'Sede Norte' },
      { id: 2, nombre: 'Sede Sur' }
    ],
    productos: [
      { id: 101, sku: 'PROD-01', nombre: 'Shampoo Profesional', stockMinimo: 10 },
      { id: 102, sku: 'PROD-02', nombre: 'Tratamiento Capilar', stockMinimo: 5 }
    ],
    existencias: [
      {
        productoId: 101,
        sku: 'PROD-01',
        productoNombre: 'Shampoo Profesional',
        sucursalId: 1,
        sucursalNombre: 'Sede Norte',
        existencia: 15,
        reservado: 2,
        disponible: 13,
        stockMinimo: 10,
        stockMaximo: 50,
        estado: 'DISPONIBLE',
        actualizadoEn: '2026-10-10T10:00:00'
      },
      {
        productoId: 101,
        sku: 'PROD-01',
        productoNombre: 'Shampoo Profesional',
        sucursalId: 2,
        sucursalNombre: 'Sede Sur',
        existencia: 4,
        reservado: 0,
        disponible: 4,
        stockMinimo: 10,
        stockMaximo: 50,
        estado: 'BAJO',
        actualizadoEn: '2026-10-10T11:00:00'
      },
      {
        productoId: 102,
        sku: 'PROD-02',
        productoNombre: 'Tratamiento Capilar',
        sucursalId: 1,
        sucursalNombre: 'Sede Norte',
        existencia: 0,
        reservado: 0,
        disponible: 0,
        stockMinimo: 5,
        stockMaximo: 25,
        estado: 'AGOTADO',
        actualizadoEn: '2026-10-09T08:00:00'
      }
    ],
    movimientos: [
      {
        id: 1,
        productoId: 101,
        productoNombre: 'Shampoo Profesional',
        sucursalId: 1,
        sucursalNombre: 'Sede Norte',
        tipo: 'ENTRADA',
        cantidad: 10,
        existenciaAnterior: 5,
        existenciaNueva: 15,
        referencia: 'FAC-1234',
        notas: 'Compra inicial',
        creadoEn: '2026-10-10T10:00:00'
      }
    ],
    tiposMovimiento: [
      { codigo: 'ENTRADA', nombre: 'Entrada', descripcion: 'Entrada de producto' },
      { codigo: 'SALIDA', nombre: 'Salida', descripcion: 'Salida de producto' },
      { codigo: 'AJUSTE', nombre: 'Conteo físico', descripcion: 'Ajuste de inventario' }
    ]
  };

  it('calcula métricas de stock físico, disponible y reservado correctamente', () => {
    const totalFisico = catalogoMock.existencias.reduce((sum, e) => sum + e.existencia, 0);
    const totalDisponible = catalogoMock.existencias.reduce((sum, e) => sum + e.disponible, 0);
    const totalReservado = catalogoMock.existencias.reduce((sum, e) => sum + e.reservado, 0);

    expect(totalFisico).toBe(19);
    expect(totalDisponible).toBe(17);
    expect(totalReservado).toBe(2);
  });

  it('identifica correctamente artículos en bajo stock y agotados', () => {
    const bajos = catalogoMock.existencias.filter(e => e.estado === 'BAJO');
    const agotados = catalogoMock.existencias.filter(e => e.estado === 'AGOTADO');

    expect(bajos.length).toBe(1);
    expect(bajos[0].sucursalNombre).toBe('Sede Sur');
    expect(agotados.length).toBe(1);
    expect(agotados[0].productoNombre).toBe('Tratamiento Capilar');
  });

  it('calcula la sugerencia de reposición para artículos en riesgo', () => {
    const enRiesgo = catalogoMock.existencias.filter(e => e.estado === 'AGOTADO' || e.estado === 'BAJO');

    const sugerencias = enRiesgo.map(e => {
      const min = Number(e.stockMinimo || 1);
      const disp = Number(e.disponible || 0);
      const unidadesFaltantes = Math.max(0, min - disp);
      const sugerido = Math.max(1, Math.ceil(min * 1.5 - disp));
      return { sku: e.sku, unidadesFaltantes, sugerido };
    });

    expect(sugerencias).toHaveLength(2);
    // Para Sede Sur (disp: 4, min: 10): faltan 6, sugerido: ceil(15 - 4) = 11
    const itemSur = sugerencias.find(s => s.sku === 'PROD-01');
    expect(itemSur?.unidadesFaltantes).toBe(6);
    expect(itemSur?.sugerido).toBe(11);

    // Para Tratamiento Capilar (disp: 0, min: 5): faltan 5, sugerido: ceil(7.5 - 0) = 8
    const itemCapilar = sugerencias.find(s => s.sku === 'PROD-02');
    expect(itemCapilar?.unidadesFaltantes).toBe(5);
    expect(itemCapilar?.sugerido).toBe(8);
  });
});
