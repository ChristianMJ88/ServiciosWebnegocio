package com.techprotech.agenda.modulos.autenticacion.social;

import jakarta.validation.constraints.NotBlank;

public record IntercambiarAccesoSocialRequest(@NotBlank String codigo, Long empresaId) {}
