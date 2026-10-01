package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearPagoAlternativoCheckoutRequest(
        @NotBlank @Size(max = 80) String metodoPagoId,
        @NotBlank @Email @Size(max = 180) String correoPagador
) {
}
