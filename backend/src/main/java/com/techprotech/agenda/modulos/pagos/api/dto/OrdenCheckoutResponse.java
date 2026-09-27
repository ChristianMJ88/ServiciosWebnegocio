package com.techprotech.agenda.modulos.pagos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenCheckoutResponse(
        String id,
        String estado,
        Long sucursalId,
        String moneda,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal impuestosTotal,
        BigDecimal costoEnvio,
        BigDecimal total,
        String modalidadEntrega,
        LocalDateTime expiraEn,
        List<Detalle> detalles
) {
    public record Detalle(String tipo, Long productoId, Long servicioId, Long citaId, String nombre,
                          BigDecimal cantidad, BigDecimal precioListaUnitario, BigDecimal precioUnitario,
                          BigDecimal descuento, BigDecimal impuestos, BigDecimal total) {}
}
