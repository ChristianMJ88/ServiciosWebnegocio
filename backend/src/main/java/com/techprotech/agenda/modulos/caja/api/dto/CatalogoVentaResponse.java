package com.techprotech.agenda.modulos.caja.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CatalogoVentaResponse(List<ProductoCajaResponse> productos) {
    public record ProductoCajaResponse(
            Long id,
            Long categoriaId,
            String sku,
            String codigoBarras,
            String nombre,
            String imagenUrl,
            BigDecimal precio,
            String moneda,
            boolean controlaInventario,
            BigDecimal disponible
    ) {}
}
