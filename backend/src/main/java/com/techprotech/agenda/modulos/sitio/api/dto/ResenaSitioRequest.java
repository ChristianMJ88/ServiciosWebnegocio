package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResenaSitioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Min(1) @Max(5) int calificacion,
        @NotBlank @Size(max = 600) String comentario,
        Long mediaId,
        @Size(max = 1000) String fotoUrl,
        boolean destacada,
        boolean activa,
        @Min(0) int orden
) {
}
