package com.techprotech.agenda.modulos.recepcion.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ReagendarRecepcionRequest(
        @NotNull @Future OffsetDateTime nuevoInicio
) {
}
