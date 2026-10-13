package com.techprotech.agenda.modulos.citas.api.dto;
import java.math.BigDecimal;
import java.util.List;

public record ResultadoCotizacionCitas(String moneda, BigDecimal subtotalLista, BigDecimal descuentoTotal,
        BigDecimal total, BigDecimal anticipoTotal, PromocionAplicada promocion, String motivoNoAplicada,
        List<LineaCotizacionCita> lineas) {
    public record PromocionAplicada(Long id, String titulo, String tipoDescuento, BigDecimal valorDescuento, String moneda) {}
    public record LineaCotizacionCita(Long servicioId, String nombre, BigDecimal precioLista,
            BigDecimal descuento, BigDecimal precioNeto, BigDecimal anticipo, boolean promocionAplicada) {}
}
