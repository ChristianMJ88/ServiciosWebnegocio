package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.constraints.*;

public record PreguntaFrecuenteSitioRequest(
        @NotBlank @Size(max = 180) String pregunta,
        @NotBlank @Size(max = 1200) String respuesta,
        boolean activa,
        @Min(0) int orden
) {}
