package com.techprotech.agenda.modulos.caja.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AbrirCajaRequest(
        @NotNull Long sucursalId,
        @NotNull @DecimalMin("0.00") BigDecimal montoInicial,
        @Size(max = 500) String observaciones
) {
}
