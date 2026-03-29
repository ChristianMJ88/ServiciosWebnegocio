package com.techprotech.agenda.modulos.admin.api.dto;

import java.time.LocalDateTime;

public record AuditoriaConfiguracionAdminResponse(
        Long id,
        String modulo,
        String accion,
        String resumen,
        String actorCorreo,
        LocalDateTime creadoEn
) {
}
