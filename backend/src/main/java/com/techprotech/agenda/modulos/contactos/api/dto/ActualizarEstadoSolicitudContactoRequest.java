package com.techprotech.agenda.modulos.contactos.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarEstadoSolicitudContactoRequest(
        @NotBlank String estado
) {
}
