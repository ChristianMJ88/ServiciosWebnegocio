package com.techprotech.agenda.modulos.contactos.api.dto;

import java.time.OffsetDateTime;

public record SolicitudContactoCreadaResponse(
        Long id,
        Long empresaId,
        String estado,
        String mensaje,
        boolean correoNotificacionProgramado,
        OffsetDateTime creadaEn
) {
}
