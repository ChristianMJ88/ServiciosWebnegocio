package com.techprotech.agenda.modulos.admin.api.dto;

public record PermisoAdminResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion
) {
}
