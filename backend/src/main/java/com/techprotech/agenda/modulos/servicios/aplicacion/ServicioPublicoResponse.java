package com.techprotech.agenda.modulos.servicios.aplicacion;

import java.math.BigDecimal;

public record ServicioPublicoResponse(
        Long id,
        Long sucursalId,
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
        BigDecimal anticipoValor
) {
}
