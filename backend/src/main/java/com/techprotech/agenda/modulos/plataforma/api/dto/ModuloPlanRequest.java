package com.techprotech.agenda.modulos.plataforma.api.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public record ModuloPlanRequest(@NotNull Long moduloId, @Min(1) Long limiteUso) {}
