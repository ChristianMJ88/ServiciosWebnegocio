package com.techprotech.agenda.modulos.onboarding.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarEmpresaRequest(
        @NotBlank @Size(min = 3, max = 150) String nombreEmpresa,
        @Size(max = 100) String slug,
        @NotBlank @Size(min = 3, max = 120) String giro,
        @NotBlank @Size(min = 3, max = 150) String nombreAdministrador,
        @NotBlank @Email @Size(max = 150) String correoAdministrador,
        @NotBlank @Size(min = 10, max = 30) String telefonoAdministrador,
        @NotBlank @Size(min = 8, max = 120) String contrasena,
        @Size(max = 60) String zonaHoraria
) {
}
