package com.techprotech.agenda.modulos.contactos.api.dto;

import java.time.OffsetDateTime;

public record SolicitudContactoAdminResponse(
        Long id,
        Long empresaId,
        String nombreCompleto,
        String telefono,
        String correo,
        String asunto,
        String mensaje,
        String canal,
        String estado,
        boolean notificacionCorreoProgramada,
        OffsetDateTime notificadaEn,
        OffsetDateTime creadaEn
) {
}
