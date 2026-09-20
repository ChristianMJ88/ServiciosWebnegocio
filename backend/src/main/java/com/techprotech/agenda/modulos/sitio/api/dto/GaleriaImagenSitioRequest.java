package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GaleriaImagenSitioRequest(
        Long mediaId,
        @NotBlank @Size(max = 500) String imagenUrl,
        @NotBlank @Size(max = 180) String textoAlternativo,
        @Size(max = 120) String titulo,
        @Size(max = 500) String descripcion,
        Long servicioId,
        Long sucursalId,
        boolean activa,
        @Min(0) int orden
) {}
