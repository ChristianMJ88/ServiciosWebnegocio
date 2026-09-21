package com.techprotech.agenda.modulos.sitio.aplicacion;

public record GaleriaImagenSitioPublicaResponse(
        Long id,
        String imagenUrl,
        String imagenUrlPequena,
        String imagenUrlMediana,
        String imagenUrlGrande,
        String imagenUrlAmpliada,
        String textoAlternativo,
        String titulo,
        String descripcion,
        Long servicioId,
        String servicioNombre,
        Long sucursalId,
        String sucursalNombre
) {}
