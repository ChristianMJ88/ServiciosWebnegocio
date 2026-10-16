package com.techprotech.agenda.modulos.caja.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
        OffsetDateTime registradoEn
) {
}