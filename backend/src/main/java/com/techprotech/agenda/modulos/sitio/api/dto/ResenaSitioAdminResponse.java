package com.techprotech.agenda.modulos.sitio.api.dto;

public record ResenaSitioAdminResponse(
        Long id,
        String nombre,
        int calificacion,
        String comentario,
        boolean destacada,
        boolean activa,
        int orden
) {
}
