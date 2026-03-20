package com.techprotech.agenda.modulos.autenticacion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarClienteRequest(
        @NotNull
        Long empresaId,
        @NotBlank @Size(min = 3, max = 150) String nombreCompleto,
        @Email @NotBlank String correo,
        @NotBlank @Pattern(regexp = "^[0-9+ ]{10,15}$") String telefono,
        @NotBlank @Size(min = 8, max = 100) String contrasena
) {
}

