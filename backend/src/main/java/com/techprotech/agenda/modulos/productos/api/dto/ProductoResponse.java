package com.techprotech.agenda.modulos.productos.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductoResponse(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        Long proveedorId,
        String proveedorNombre,
        Long mediaId,
        String sku,
        String codigoBarras,
        String nombre,
        String slug,
        String descripcion,
        String imagenUrl,
        BigDecimal precioBase,
        BigDecimal costo,
        BigDecimal pesoKg,
        BigDecimal largoCm,
        BigDecimal anchoCm,
        BigDecimal altoCm,
        String moneda,
        BigDecimal tasaImpuesto,
        boolean controlaInventario,
        BigDecimal stockMinimo,
        boolean visiblePublico,
        boolean activo,
        List<PrecioZonaProductoResponse> preciosZona
) {}
