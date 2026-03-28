package com.techprotech.agenda.modulos.caja.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegistrarPagoRequest(
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @NotBlank @Size(max = 30) String metodoPago,
        @Size(max = 120) String referencia,
        @Size(max = 500) String observaciones
) {
}
