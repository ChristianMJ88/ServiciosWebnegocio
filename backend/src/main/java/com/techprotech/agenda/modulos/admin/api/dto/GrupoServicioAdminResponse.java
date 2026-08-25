package com.techprotech.agenda.modulos.admin.api.dto;

public record GrupoServicioAdminResponse(
        Long id,
        String nombre,
        String slug,
        String descripcion,
        String imagenUrl,
        String icono,
        int ordenPublico,
        boolean activo
) {
}
