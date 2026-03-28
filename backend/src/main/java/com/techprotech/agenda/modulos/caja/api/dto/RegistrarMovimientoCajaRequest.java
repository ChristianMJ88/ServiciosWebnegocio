package com.techprotech.agenda.modulos.caja.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegistrarMovimientoCajaRequest(
        @NotNull Long sucursalId,
        @NotBlank @Size(max = 30) String tipoMovimiento,
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @Size(max = 30) String metodoPago,
        @NotBlank @Size(max = 255) String concepto,
        @Size(max = 120) String referencia,
        @Size(max = 500) String observaciones
) {
}
