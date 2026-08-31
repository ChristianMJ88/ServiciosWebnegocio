package com.techprotech.agenda.modulos.onboarding.api.dto;
import jakarta.validation.constraints.NotBlank;
public record ConfirmarCorreoOnboardingRequest(@NotBlank String token) {}
