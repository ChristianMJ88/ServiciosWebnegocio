package com.techprotech.agenda.compartido.correo;

import java.time.OffsetDateTime;

public record NotificacionSolicitudContactoCorreo(
        Long solicitudContactoId,
        String nombreEmpresa,
        String nombreContacto,
        String correoContacto,
        String telefonoContacto,
        String asunto,
        String mensaje,
        String canal,
        OffsetDateTime creadaEn
) {
}
