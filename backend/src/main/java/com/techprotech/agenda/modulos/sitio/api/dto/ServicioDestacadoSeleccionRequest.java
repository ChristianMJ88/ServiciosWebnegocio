package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicioDestacadoSeleccionRequest(
        @NotBlank String tipo,
        @NotNull Long referenciaId,
        @Size(max = 40) String etiqueta,
        @Min(0) int orden
) {
}
