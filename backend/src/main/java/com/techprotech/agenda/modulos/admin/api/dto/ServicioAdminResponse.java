package com.techprotech.agenda.modulos.admin.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ServicioAdminResponse(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        List<Long> sucursalIds,
        List<String> sucursalNombres,
        Long grupoId,
        String grupoNombre,
        Long subgrupoId,
        String subgrupoNombre,
        String nombre,
        String slug,
        String descripcion,
        String imagenUrl,
        int duracionMinutos,
        int bufferAntesMinutos,
        int bufferDespuesMinutos,
        BigDecimal precio,
        String moneda,
        int ordenPublico,
        boolean visiblePublico,
        boolean requiereAnticipo,
        String anticipoTipo,
        BigDecimal anticipoValor,
        boolean activo
) {
}
