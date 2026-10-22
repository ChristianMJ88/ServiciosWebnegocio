package com.techprotech.agenda.modulos.dominios.api.dto;
import jakarta.validation.constraints.*;
public record VersionDominioSolicitud(@NotNull @PositiveOrZero Long version) {}
