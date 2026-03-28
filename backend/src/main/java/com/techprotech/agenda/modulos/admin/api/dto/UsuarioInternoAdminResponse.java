package com.techprotech.agenda.modulos.admin.api.dto;

public record UsuarioInternoAdminResponse(
        Long usuarioId,
        Long sucursalId,
        String sucursalNombre,
        String correo,
        String nombreCompleto,
        String telefono,
        String puesto,
        String rolCodigo,
        boolean activo,
        String notas
) {
}
