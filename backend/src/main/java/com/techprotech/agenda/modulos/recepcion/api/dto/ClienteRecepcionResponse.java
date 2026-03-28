package com.techprotech.agenda.modulos.recepcion.api.dto;

public record ClienteRecepcionResponse(
        Long usuarioId,
        String nombreCompleto,
        String telefono,
        String correo,
        boolean aceptaWhatsapp
) {
}
