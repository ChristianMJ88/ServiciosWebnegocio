package com.techprotech.agenda.modulos.sitio.api.dto;

public record ServicioDestacadoSeleccionAdminResponse(
        String tipo,
        Long referenciaId,
        String nombre,
        String rutaCatalogo,
        String etiqueta,
        int orden,
        boolean disponible
) {
}
