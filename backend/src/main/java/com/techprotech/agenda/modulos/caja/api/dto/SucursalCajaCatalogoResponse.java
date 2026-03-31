package com.techprotech.agenda.modulos.caja.api.dto;

public record SucursalCajaCatalogoResponse(
        Long id,
        Long empresaId,
        String nombre,
        String direccion,
        String telefono,
        String zonaHoraria
) {
}
