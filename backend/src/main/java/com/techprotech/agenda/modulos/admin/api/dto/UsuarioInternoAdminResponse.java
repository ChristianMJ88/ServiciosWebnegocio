package com.techprotech.agenda.modulos.admin.api.dto;

import java.util.List;

public record UsuarioInternoAdminResponse(
        Long usuarioId,
        Long sucursalId,
        String sucursalNombre,
        List<Long> sucursalIds,
        List<String> sucursalNombresScope,
        String correo,
        String nombreCompleto,
        String telefono,
        String puesto,
        Long rolEmpresaId,
        String rolCodigo,
        String rolNombre,
        List<String> permisosRol,
        List<String> permisosDirectos,
        List<String> permisosEfectivos,
        boolean activo,
        String notas
) {
}
