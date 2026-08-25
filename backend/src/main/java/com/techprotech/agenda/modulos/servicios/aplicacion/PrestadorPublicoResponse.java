package com.techprotech.agenda.modulos.servicios.aplicacion;

public record PrestadorPublicoResponse(
        Long usuarioId,
        Long sucursalId,
        String nombreMostrar,
        String biografia,
        String colorAgenda
) {
}
