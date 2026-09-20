package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BeneficioSitioRequest(
        @NotBlank @Size(max = 120) String titulo,
        @NotBlank @Size(max = 400) String descripcion,
        @NotBlank @Size(max = 30) String icono,
        boolean activo,
        @Min(0) int orden
) {
}
