package com.techprotech.agenda.modulos.onboarding.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record RegistrarEmpresaRequest(
        @NotBlank @Size(min = 3, max = 150) String nombreEmpresa,
        @Size(max = 100) String slug,
        @NotBlank @Size(min = 3, max = 120) String giro,
        @NotBlank @Size(max = 30) String tamanoEquipo,
        @NotBlank @Size(min = 3, max = 150) String nombreAdministrador,
        @NotBlank @Email @Size(max = 150) @Pattern(regexp = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$", message = "El correo debe incluir un dominio válido") String correoAdministrador,
        @NotBlank @Size(min = 10, max = 30) String telefonoAdministrador,
        @Size(min = 8, max = 120) String contrasena,
        @Size(max = 60) String zonaHoraria,
        String registroSocialToken
) {
}
