package com.techprotech.agenda.modulos.whatsapp.onboarding.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IniciarOnboardingWhatsappRequest(
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String telefonoE164,
        @NotBlank @Size(max = 150) String displayName
) {}
