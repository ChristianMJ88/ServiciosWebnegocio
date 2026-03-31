package com.techprotech.agenda.modulos.recepcion.api.dto;

public record SucursalRecepcionCatalogoResponse(
        Long id,
        Long empresaId,
        String nombre,
        String direccion,
        String telefono,
        String zonaHoraria
) {
}
