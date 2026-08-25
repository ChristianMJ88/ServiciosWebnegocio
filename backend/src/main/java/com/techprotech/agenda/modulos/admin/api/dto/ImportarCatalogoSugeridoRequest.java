package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImportarCatalogoSugeridoRequest(
        @NotBlank String sugerenciaId,
        @NotNull Long sucursalId
) {
}
