package com.techprotech.agenda.modulos.sitio.api.dto;

public record GaleriaImagenSitioAdminResponse(
        Long id,
        Long mediaId,
        String imagenUrl,
        String textoAlternativo,
        String titulo,
        String descripcion,
        Long servicioId,
        String servicioNombre,
        Long sucursalId,
        String sucursalNombre,
        boolean activa,
        int orden
) {}
