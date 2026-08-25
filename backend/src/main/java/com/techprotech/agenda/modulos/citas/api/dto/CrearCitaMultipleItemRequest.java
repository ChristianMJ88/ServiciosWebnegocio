package com.techprotech.agenda.modulos.citas.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CrearCitaMultipleItemRequest(
        @NotNull Long servicioId,
        Long prestadorId,
        @NotNull @Future OffsetDateTime inicio
) {
}
