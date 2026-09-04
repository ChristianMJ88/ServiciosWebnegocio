package com.techprotech.agenda.modulos.clientes.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarClienteRequest(
        @NotBlank @Size(max = 150) String nombreCompleto,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(max = 30) String telefono,
        boolean aceptaWhatsapp,
        @Size(max = 500) String notas
) {
}
