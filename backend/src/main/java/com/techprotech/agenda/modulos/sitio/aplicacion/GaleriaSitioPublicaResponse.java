package com.techprotech.agenda.modulos.sitio.aplicacion;

import java.util.List;

public record GaleriaSitioPublicaResponse(
        String titulo,
        String descripcion,
        String vista,
        boolean ampliacionHabilitada,
        List<GaleriaImagenSitioPublicaResponse> imagenes
) {}
