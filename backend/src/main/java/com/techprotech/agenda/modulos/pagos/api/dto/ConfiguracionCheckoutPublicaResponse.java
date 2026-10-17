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
        boolean mostrarEstadoPago,
        boolean permitirPagoProductos,
        boolean permitirPagoServicios,
        boolean permitirRecoleccion,
        boolean permitirEntregaLocal,
        BigDecimal costoEntregaLocal,
        boolean permitirEnvioNacional,
        BigDecimal costoEnvioNacional,
        boolean cotizacionEnvioDisponible,
        String modalidadServicios,
        BigDecimal anticipoPorcentaje,
        BigDecimal anticipoImporte,
        String moneda,
        Politica politica,
        boolean productosOnlineDisponibles,
        String motivoProductosNoDisponibles
) {
    public ConfiguracionCheckoutPublicaResponse(boolean habilitado,
        String proveedor,
        String ambiente,
        String llavePublica,
        boolean aceptarTarjeta,
        boolean aceptarTransferencia,
        boolean aceptarEfectivoReferenciado,
        boolean mostrarEstadoPago,
        boolean permitirPagoProductos,
        boolean permitirPagoServicios,
        boolean permitirRecoleccion,
        boolean permitirEntregaLocal,
        BigDecimal costoEntregaLocal,
        boolean permitirEnvioNacional,
        BigDecimal costoEnvioNacional,
        boolean cotizacionEnvioDisponible,
        String modalidadServicios,
        BigDecimal anticipoPorcentaje,
        BigDecimal anticipoImporte,
        String moneda,
        Politica politica) {
        this(habilitado, proveedor, ambiente, llavePublica, aceptarTarjeta, aceptarTransferencia, aceptarEfectivoReferenciado, mostrarEstadoPago, permitirPagoProductos, permitirPagoServicios, permitirRecoleccion, permitirEntregaLocal, costoEntregaLocal, permitirEnvioNacional, costoEnvioNacional, cotizacionEnvioDisponible, modalidadServicios, anticipoPorcentaje, anticipoImporte, moneda, politica, habilitado && permitirPagoProductos && (aceptarTarjeta || aceptarTransferencia || aceptarEfectivoReferenciado), null);
    }
    public record Politica(Integer version, java.time.OffsetDateTime vigenteDesde) {}
}
