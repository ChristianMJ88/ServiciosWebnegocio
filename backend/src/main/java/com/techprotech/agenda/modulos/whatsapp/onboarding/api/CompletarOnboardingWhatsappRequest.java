package com.techprotech.agenda.modulos.whatsapp.onboarding.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompletarOnboardingWhatsappRequest(
        @NotBlank @Size(max = 36) String onboardingId,
        @NotBlank @Size(max = 100) String wabaId,
        @Size(max = 100) String phoneNumberId,
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String telefonoE164
) {}
