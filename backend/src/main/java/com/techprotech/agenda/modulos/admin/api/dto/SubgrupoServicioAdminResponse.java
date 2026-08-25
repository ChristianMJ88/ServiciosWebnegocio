package com.techprotech.agenda.modulos.admin.api.dto;

public record SubgrupoServicioAdminResponse(
        Long id,
        Long grupoId,
        String grupoNombre,
        String nombre,
        String slug,
        String descripcion,
        int ordenPublico,
        boolean activo
) {
}
