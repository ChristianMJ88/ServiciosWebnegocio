package com.techprotech.agenda.modulos.productos.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PrecioZonaProductoRequest(
        @NotNull Long zonaPrecioId,
        @NotNull @DecimalMin("0.0") BigDecimal precio,
        @NotBlank @Size(max = 10) String moneda,
        boolean activo
) {}
