package com.techprotech.agenda.modulos.envios.api.dto;

import java.util.List;

public record OnboardingEnvioResponse(
        boolean conexionAutomaticaDisponible,
        String registroUrl,
        String credencialesUrl,
        List<ModoCredencialEnvioResponse> modosCredencial
) {}
