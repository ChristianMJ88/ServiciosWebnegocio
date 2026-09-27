package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ConfiguracionCheckoutRequest(
        boolean habilitado,
        boolean aceptarTarjeta,
        boolean aceptarTransferencia,
        boolean aceptarEfectivoReferenciado,
        boolean permitirPagoProductos,
        boolean permitirPagoServicios,
        boolean permitirRecoleccion,
        boolean permitirEntregaLocal,
        @DecimalMin("0.00") BigDecimal costoEntregaLocal,
        boolean permitirEnvioNacional,
        @DecimalMin("0.00") BigDecimal costoEnvioNacional,
        @Pattern(regexp = "PAGO_COMPLETO|ANTICIPO_PORCENTAJE|ANTICIPO_IMPORTE") String modalidadServicios,
        @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal anticipoPorcentaje,
        @DecimalMin("0.01") BigDecimal anticipoImporte,
        @Pattern(regexp = "[A-Z]{3}") String moneda
) {
}
