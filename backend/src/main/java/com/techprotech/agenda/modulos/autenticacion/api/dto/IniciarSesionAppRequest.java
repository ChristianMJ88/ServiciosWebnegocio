package com.techprotech.agenda.modulos.autenticacion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IniciarSesionAppRequest(
        @Email @NotBlank @Pattern(regexp = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$", message = "El correo debe incluir un dominio válido") String correo,
        @NotBlank String contrasena,
        Long empresaId
) {
}
