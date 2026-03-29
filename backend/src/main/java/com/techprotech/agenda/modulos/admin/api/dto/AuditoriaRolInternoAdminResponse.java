package com.techprotech.agenda.modulos.admin.api.dto;

import java.time.LocalDateTime;

public record AuditoriaRolInternoAdminResponse(
        Long id,
        String accion,
        String resumen,
        String actorCorreo,
        String rolCodigo,
        String rolNombre,
        LocalDateTime creadoEn
) {
}
