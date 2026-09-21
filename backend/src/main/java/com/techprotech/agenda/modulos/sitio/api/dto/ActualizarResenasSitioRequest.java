package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ActualizarResenasSitioRequest(
        @Size(max = 500) String googleMapsUrl,
        @NotNull @Size(max = 20) List<@Valid ResenaSitioRequest> resenas,
        @NotNull @Size(max = 4) List<@Valid IndicadorConfianzaSitioRequest> indicadores
) {
}
