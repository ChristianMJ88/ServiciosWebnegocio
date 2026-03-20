package com.techprotech.agenda.modulos.citas.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ReprogramarCitaClienteRequest(
        @NotNull @Future OffsetDateTime nuevoInicio
) {
}
