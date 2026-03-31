package com.techprotech.agenda.modulos.recepcion.api.dto;

import java.math.BigDecimal;

public record ServicioRecepcionCatalogoResponse(
        Long id,
        Long sucursalId,
        String nombre,
        String descripcion,
        int duracionMinutos,
        int bufferAntesMinutos,
        int bufferDespuesMinutos,
        BigDecimal precio,
        String moneda
) {
}
