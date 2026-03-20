package com.techprotech.agenda.modulos.admin.api.dto;

public record SucursalAdminResponse(
        Long id,
        String nombre,
        String direccion,
        String telefono,
        String zonaHoraria,
        boolean activa

) {
}
