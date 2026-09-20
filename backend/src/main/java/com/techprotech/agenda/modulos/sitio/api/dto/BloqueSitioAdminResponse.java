package com.techprotech.agenda.modulos.sitio.api.dto;

public record BloqueSitioAdminResponse(
        String tipo,
        String nombre,
        String descripcion,
        String icono,
        boolean habilitado,
        int orden,
        boolean disponible,
        String superficie,
        String presentacion,
        String anchoContenido,
        String alineacion,
        String separador,
        String espaciado,
        String etiqueta,
        String titulo,
        String descripcionVisual
) {
}
