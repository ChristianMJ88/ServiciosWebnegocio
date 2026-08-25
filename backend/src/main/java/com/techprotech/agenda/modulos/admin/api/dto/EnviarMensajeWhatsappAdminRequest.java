package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeWhatsappAdminRequest(
        @NotBlank @Size(max = 30) String telefono,
        @NotBlank @Size(max = 1600) String mensaje
) {
}
