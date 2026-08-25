package com.techprotech.agenda.modulos.autenticacion.api.dto;

public record PerfilUsuarioResponse(
        Long usuarioId,
        String correo,
        String nombreCompleto,
        String puesto,
        Long sucursalId
) {
}
