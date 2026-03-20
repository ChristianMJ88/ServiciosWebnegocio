package com.techprotech.agenda.modulos.admin.api.dto;

import java.math.BigDecimal;

public record ServicioAdminResponse(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        String nombre,
        String descripcion,
        int duracionMinutos,
        int bufferAntesMinutos,
        int bufferDespuesMinutos,
        BigDecimal precio,
        String moneda,
        boolean activo
) {
}
