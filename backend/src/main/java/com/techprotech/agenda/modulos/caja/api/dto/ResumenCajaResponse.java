package com.techprotech.agenda.modulos.caja.api.dto;

import java.math.BigDecimal;

public record ResumenCajaResponse(
        CajaSesionResponse sesion,
        BigDecimal totalCobrado,
        BigDecimal totalCobradoEfectivo,
        BigDecimal totalCobradoTarjeta,
        BigDecimal totalCobradoTransferencia,
        BigDecimal totalGastos,
        BigDecimal totalRetiros,
        BigDecimal saldoEsperadoCaja,
        long citasPendientesDeCobro
) {
}
