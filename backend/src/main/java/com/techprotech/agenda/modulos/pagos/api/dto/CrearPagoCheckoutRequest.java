package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearPagoCheckoutRequest(
        @NotBlank @Size(max = 600) String tokenMedioPago,
        @NotBlank @Size(max = 80) String metodoPagoId,
        @Size(max = 40) String tipoPagoId,
        @Min(1) @Max(48) int parcialidades,
        @NotBlank @Email @Size(max = 180) String correoPagador
) {
}
