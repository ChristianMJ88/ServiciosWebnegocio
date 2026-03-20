package com.techprotech.agenda.modulos.autenticacion.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RefrescarTokenRequest(
        @NotBlank String tokenActualizacion
) {
}
