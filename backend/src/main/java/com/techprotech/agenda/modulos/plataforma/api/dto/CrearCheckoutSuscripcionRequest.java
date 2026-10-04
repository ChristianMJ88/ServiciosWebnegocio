package com.techprotech.agenda.modulos.plataforma.api.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
public record CrearCheckoutSuscripcionRequest(@NotNull Long planId,
        @Pattern(regexp="MENSUAL|ANUAL") String ciclo) {}
