package com.techprotech.agenda.modulos.productos.api.dto;

import java.math.BigDecimal;

public record PrecioZonaProductoResponse(
        Long id, Long zonaPrecioId, String zonaNombre, BigDecimal precio, String moneda, boolean activo
) {}
