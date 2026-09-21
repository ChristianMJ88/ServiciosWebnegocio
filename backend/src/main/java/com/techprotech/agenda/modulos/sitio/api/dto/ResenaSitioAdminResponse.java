package com.techprotech.agenda.modulos.sitio.api.dto;

public record ResenaSitioAdminResponse(
        Long id,
        String nombre,
        int calificacion,
        String comentario,
        Long mediaId,
        String fotoUrl,
        boolean destacada,
        boolean activa,
        int orden
) {
}
