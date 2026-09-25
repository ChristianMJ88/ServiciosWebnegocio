package com.techprotech.agenda.modulos.productos.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CatalogoInventarioResponse(
        List<SucursalInventario> sucursales,
        List<ProductoInventario> productos,
        List<ExistenciaProductoResponse> existencias,
        List<MovimientoInventarioResponse> movimientos,
        List<TipoMovimientoInventario> tiposMovimiento
) {
    public record SucursalInventario(Long id, String nombre) {}
    public record ProductoInventario(Long id, String sku, String nombre, BigDecimal stockMinimo) {}
    public record TipoMovimientoInventario(String codigo, String nombre, String descripcion) {}
}
