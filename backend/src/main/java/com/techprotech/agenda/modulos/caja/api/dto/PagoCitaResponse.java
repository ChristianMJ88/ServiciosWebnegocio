package com.techprotech.agenda.modulos.caja.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoCitaResponse(
        Long id,
        Long citaId,
        Long cajaSesionId,
        BigDecimal monto,
        String metodoPago,
        String referencia,
        String observaciones,
        Long registradoPorUsuarioId,
        LocalDateTime registradoEn
) {
}
