package com.techprotech.agenda.modulos.admin.api.dto;

import java.util.List;

public record PrestadorAdminResponse(
        Long usuarioId,
        Long sucursalId,
        String sucursalNombre,
        String correo,
        String nombreMostrar,
        String biografia,
        String colorAgenda,
        boolean activo,
        List<Long> servicioIds,
        List<String> servicioNombres
) {
}
