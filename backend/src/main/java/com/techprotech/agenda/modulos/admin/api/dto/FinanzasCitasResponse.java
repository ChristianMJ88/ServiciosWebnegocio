package com.techprotech.agenda.modulos.admin.api.dto;

import java.math.BigDecimal;

/** Importes de citas por moneda; la custodia requiere la persistencia de la entrega 007. */
public record FinanzasCitasResponse(String moneda, BigDecimal ingresosProgramados, BigDecimal ingresosDevengados,
        BigDecimal ingresosCobrados, BigDecimal saldoPorCobrar, BigDecimal anticiposEnCustodia,
        BigDecimal ingresosAnticiposVencidos, boolean custodiaDisponible) {}
