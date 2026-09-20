package com.techprotech.agenda.modulos.sitio.api.dto;

import java.util.List;

public record GaleriaSitioAdminResponse(
        String titulo,
        String descripcion,
        String vista,
        boolean ampliacionHabilitada,
        List<GaleriaImagenSitioAdminResponse> imagenes
) {}
