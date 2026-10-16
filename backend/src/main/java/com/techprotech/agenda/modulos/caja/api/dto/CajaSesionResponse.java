package com.techprotech.agenda.modulos.caja.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CajaSesionResponse(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        String estado,
        BigDecimal montoInicial,
        BigDecimal montoEsperado,
        BigDecimal montoContado,
        BigDecimal diferencia,
        String observaciones,
        OffsetDateTime abiertaEn,
        OffsetDateTime cerradaEn
) {
}