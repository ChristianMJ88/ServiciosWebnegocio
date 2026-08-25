package com.techprotech.agenda.modulos.admin.api.dto;

import java.time.LocalDateTime;

public record PlantillaWhatsappEmpresaAdminResponse(
        Long id,
        String nombre,
        String uso,
        String contentSid,
        String tipoContenido,
        String categoria,
        String estado,
        boolean activa,
        LocalDateTime creadaEn,
        LocalDateTime actualizadaEn
) {
}
