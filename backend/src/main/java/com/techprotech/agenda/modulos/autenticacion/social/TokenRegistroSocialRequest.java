package com.techprotech.agenda.modulos.autenticacion.social;

import jakarta.validation.constraints.NotBlank;

public record TokenRegistroSocialRequest(@NotBlank String token) {
}
