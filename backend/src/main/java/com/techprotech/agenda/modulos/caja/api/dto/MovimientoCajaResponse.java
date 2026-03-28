package com.techprotech.agenda.modulos.caja.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoCajaResponse(
        Long id,
        Long cajaSesionId,
        Long citaId,
        String tipoMovimiento,
        String metodoPago,
        BigDecimal monto,
        String concepto,
        String referencia,
        String observaciones,
        Long registradoPorUsuarioId,
        LocalDateTime registradoEn
) {
}
