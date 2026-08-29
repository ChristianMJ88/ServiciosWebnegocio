package com.techprotech.agenda.modulos.whatsapp.onboarding.api;

import java.time.LocalDateTime;

public record OnboardingWhatsappResponse(
        String onboardingId,
        String estado,
        String pasoActual,
        String telefonoE164,
        String displayName,
        String wabaId,
        String phoneNumberId,
        String channelSenderSid,
        String ultimoError,
        LocalDateTime actualizadoEn,
        boolean embeddedSignupDisponible,
        String metaAppId,
        String configurationId,
        String partnerSolutionId
) {}
