package com.techprotech.agenda.modulos.contactos.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearSolicitudContactoRequest(
        Long empresaId,
        @NotBlank @Size(min = 3, max = 150) String nombreCompleto,
        @Size(max = 30) @Pattern(regexp = "^$|^[0-9+()\\- ]{10,20}$") String telefono,
        @Email @NotBlank @Size(max = 150) String correo,
        @NotBlank @Size(min = 3, max = 180) String asunto,
        @NotBlank @Size(min = 10, max = 2000) String mensaje
) {
}
