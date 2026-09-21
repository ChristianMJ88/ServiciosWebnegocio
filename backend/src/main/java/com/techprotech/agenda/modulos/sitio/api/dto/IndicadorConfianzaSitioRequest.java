package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IndicadorConfianzaSitioRequest(
        @NotBlank @Size(max = 40) String valor,
        @NotBlank @Size(max = 80) String etiqueta,
        @NotBlank @Size(max = 30) String icono,
        boolean activo,
        @Min(0) int orden
) {
}
