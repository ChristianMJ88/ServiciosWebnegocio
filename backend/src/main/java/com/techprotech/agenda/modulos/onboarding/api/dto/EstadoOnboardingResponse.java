package com.techprotech.agenda.modulos.onboarding.api.dto;

import java.util.List;

public record EstadoOnboardingResponse(
        boolean correoVerificado,
        String correo,
        String categoria,
        String tamanoEquipo,
        String pasoRecomendado,
        int pasosCompletados,
        int totalPasos,
        List<String> pasosPendientes
) {
}
