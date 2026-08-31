package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record AceptarInvitacionUsuarioRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 10, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{10,100}$",
                message = "La contraseña debe incluir mayúscula, minúscula, número y símbolo")
        String contrasena,
        String registroSocialToken
) {}
