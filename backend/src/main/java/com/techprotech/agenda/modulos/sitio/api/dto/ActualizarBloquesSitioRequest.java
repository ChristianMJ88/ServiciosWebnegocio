package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ActualizarBloquesSitioRequest(
        @NotEmpty List<@Valid BloqueSitioRequest> bloques
) {
}
