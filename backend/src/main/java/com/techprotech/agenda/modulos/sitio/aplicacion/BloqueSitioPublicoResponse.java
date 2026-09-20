package com.techprotech.agenda.modulos.sitio.aplicacion;

public record BloqueSitioPublicoResponse(
        String tipo,
        boolean habilitado,
        int orden,
        String superficie,
        String presentacion,
        String anchoContenido,
        String alineacion,
        String separador,
        String espaciado,
        String etiqueta,
        String titulo,
        String descripcion
) {
}
