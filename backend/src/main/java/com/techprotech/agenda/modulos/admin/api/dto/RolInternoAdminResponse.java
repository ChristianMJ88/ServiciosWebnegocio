package com.techprotech.agenda.modulos.admin.api.dto;

import java.util.List;

public record RolInternoAdminResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        boolean editable,
        long usuariosAsignados,
        boolean sePuedeEliminar,
        List<String> permisos
) {
}
