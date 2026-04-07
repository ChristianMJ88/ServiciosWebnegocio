package com.techprotech.agenda.modulos.autenticacion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record IniciarSesionAppRequest(
        @Email @NotBlank String correo,
        @NotBlank String contrasena,
        Long empresaId
) {
}
