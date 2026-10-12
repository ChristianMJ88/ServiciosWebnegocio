package com.techprotech.agenda.modulos.caja.api.dto;
import java.math.BigDecimal;
public record CotizacionCajaResponse(BigDecimal subtotal, BigDecimal descuento, BigDecimal total, java.util.List<Linea> lineas) {
    public record Linea(Long productoId, BigDecimal precioUnitario, BigDecimal total) {}
}
