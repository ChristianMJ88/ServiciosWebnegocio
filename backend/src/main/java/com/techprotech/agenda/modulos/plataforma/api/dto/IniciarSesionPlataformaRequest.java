package com.techprotech.agenda.modulos.plataforma.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IniciarSesionPlataformaRequest(
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(max = 200) String contrasena) {
}
