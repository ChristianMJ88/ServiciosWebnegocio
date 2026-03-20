package com.techprotech.agenda.modulos.autenticacion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IniciarSesionRequest(
        @NotNull Long empresaId,
        @Email @NotBlank String correo,
        @NotBlank String contrasena
) {
}

