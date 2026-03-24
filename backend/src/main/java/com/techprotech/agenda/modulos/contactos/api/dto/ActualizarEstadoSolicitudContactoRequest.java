package com.techprotech.agenda.modulos.contactos.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarEstadoSolicitudContactoRequest(
        @NotBlank
        @Pattern(regexp = "NUEVO|EN_PROCESO|ATENDIDO|CERRADO")
        String estado
) {
}
