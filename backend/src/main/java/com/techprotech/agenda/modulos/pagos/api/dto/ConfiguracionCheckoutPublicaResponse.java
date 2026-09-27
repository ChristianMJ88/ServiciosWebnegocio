package com.techprotech.agenda.modulos.pagos.api.dto;

import java.math.BigDecimal;

public record ConfiguracionCheckoutPublicaResponse(
        boolean habilitado,
        String proveedor,
        String ambiente,
        String llavePublica,
        boolean aceptarTarjeta,
        boolean aceptarTransferencia,
        boolean aceptarEfectivoReferenciado,
        boolean permitirPagoProductos,
        boolean permitirPagoServicios,
        boolean permitirRecoleccion,
        boolean permitirEntregaLocal,
        BigDecimal costoEntregaLocal,
        boolean permitirEnvioNacional,
        BigDecimal costoEnvioNacional,
        String modalidadServicios,
        BigDecimal anticipoPorcentaje,
        BigDecimal anticipoImporte,
        String moneda
) {
}
