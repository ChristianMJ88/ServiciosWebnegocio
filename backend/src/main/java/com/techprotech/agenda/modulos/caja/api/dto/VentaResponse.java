package com.techprotech.agenda.modulos.caja.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record VentaResponse(
        Long id,
        String folio,
        Long sucursalId,
        Long citaId,
        Long clienteId,
        String estado,
        String moneda,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal impuestosTotal,
        BigDecimal total,
        String metodoPago,
        String referencia,
        OffsetDateTime creadaEn,
        String canal,
        List<DetalleVentaResponse> detalles
) {
    public record DetalleVentaResponse(
            Long id,
            String tipoConcepto,
            Long productoId,
            Long servicioId,
            Long citaId,
            String sku,
            String nombre,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal impuestos,
            BigDecimal total,
            BigDecimal costoUnitarioHistorico
    ) {}
}
