package com.techprotech.agenda.modulos.sitio.aplicacion;

public record ResenaSitioPublicaResponse(
        Long id,
        String nombre,
        int calificacion,
        String comentario,
        String fotoUrl,
        boolean destacada
) {
}
