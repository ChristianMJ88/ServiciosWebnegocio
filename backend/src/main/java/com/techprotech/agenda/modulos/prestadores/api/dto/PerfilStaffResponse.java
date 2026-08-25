package com.techprotech.agenda.modulos.prestadores.api.dto;

public record PerfilStaffResponse(
        Long usuarioId,
        Long sucursalId,
        String nombreMostrar,
        String biografia,
        String colorAgenda,
        boolean activo
) {
}
