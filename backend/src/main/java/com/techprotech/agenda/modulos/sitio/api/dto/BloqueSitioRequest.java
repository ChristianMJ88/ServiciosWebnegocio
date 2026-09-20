package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BloqueSitioRequest(
        @NotBlank String tipo,
        @NotNull Boolean habilitado,
        @Min(0) int orden,
        @NotBlank String superficie,
        @NotBlank String presentacion,
        @NotBlank String anchoContenido,
        @NotBlank String alineacion,
        @NotBlank String separador,
        @NotBlank String espaciado,
        @Size(max = 80) String etiqueta,
        @Size(max = 160) String titulo,
        @Size(max = 400) String descripcionVisual
) {
}
