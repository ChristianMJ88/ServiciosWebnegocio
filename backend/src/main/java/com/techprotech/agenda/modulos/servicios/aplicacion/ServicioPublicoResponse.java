package com.techprotech.agenda.modulos.servicios.aplicacion;

import java.math.BigDecimal;

public record ServicioPublicoResponse(
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

