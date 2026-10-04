package com.techprotech.agenda.modulos.plataforma.api.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public record LimitePlanRequest(@NotNull Long limiteId,@NotNull @Min(0) Long valor) {}
