package com.techprotech.agenda.modulos.sucursales.aplicacion;

public record SucursalPublicaResponse(
        Long id,
        Long empresaId,
        String nombre,
        String direccion,
        String telefono,
        String zonaHoraria
) {
}

