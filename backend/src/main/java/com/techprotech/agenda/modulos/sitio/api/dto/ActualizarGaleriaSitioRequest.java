package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ActualizarGaleriaSitioRequest(
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 500) String descripcion,
        @NotBlank @Size(max = 20) String vista,
        boolean ampliacionHabilitada,
        @NotNull @Size(max = 60) List<@Valid GaleriaImagenSitioRequest> imagenes
) {}
