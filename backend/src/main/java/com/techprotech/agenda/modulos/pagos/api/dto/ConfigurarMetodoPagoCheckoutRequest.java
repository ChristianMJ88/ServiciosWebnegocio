package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigurarMetodoPagoCheckoutRequest(
        @NotBlank @Size(max = 80) String metodoId,
        boolean activo,
        boolean soloProductos,
        @Min(1) @Max(30) int vigenciaDias
) {
}
