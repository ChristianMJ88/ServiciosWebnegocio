package com.techprotech.agenda.modulos.citas.api.dto;
import java.math.BigDecimal;
import java.util.List;

public record ResultadoCotizacionCitas(String moneda, BigDecimal subtotalLista, BigDecimal descuentoTotal,
        BigDecimal total, BigDecimal anticipoTotal, PromocionAplicada promocion, String motivoNoAplicada,
        List<LineaCotizacionCita> lineas, boolean requierePago, BigDecimal importeRequeridoTotal,
        List<com.techprotech.agenda.modulos.productos.aplicacion.ServicioCotizacionProductos.CotizacionProducto> productos, boolean requierePagoProductos, String avisoProductos,
        boolean reservableEnLinea, String motivoNoReservable) {
    public ResultadoCotizacionCitas(String moneda, BigDecimal subtotalLista, BigDecimal descuentoTotal, BigDecimal total, BigDecimal anticipoTotal, PromocionAplicada promocion, String motivoNoAplicada, List<LineaCotizacionCita> lineas, boolean requierePago, BigDecimal importeRequeridoTotal, List<com.techprotech.agenda.modulos.productos.aplicacion.ServicioCotizacionProductos.CotizacionProducto> productos, boolean requierePagoProductos, String avisoProductos) {
        this(moneda, subtotalLista, descuentoTotal, total, anticipoTotal, promocion, motivoNoAplicada, lineas, requierePago, importeRequeridoTotal, productos, requierePagoProductos, avisoProductos, true, null);
    }
    public ResultadoCotizacionCitas(String moneda,BigDecimal subtotalLista,BigDecimal descuentoTotal,BigDecimal total,BigDecimal anticipoTotal,PromocionAplicada promocion,String motivoNoAplicada,List<LineaCotizacionCita> lineas,boolean requierePago,BigDecimal importeRequeridoTotal) { this(moneda,subtotalLista,descuentoTotal,total,anticipoTotal,promocion,motivoNoAplicada,lineas,requierePago,importeRequeridoTotal,List.of(),false,null); }
    public ResultadoCotizacionCitas(String moneda,BigDecimal subtotalLista,BigDecimal descuentoTotal,BigDecimal total,BigDecimal anticipoTotal,PromocionAplicada promocion,String motivoNoAplicada,List<LineaCotizacionCita> lineas) { this(moneda,subtotalLista,descuentoTotal,total,anticipoTotal,promocion,motivoNoAplicada,lineas,false,anticipoTotal); }
    public record PromocionAplicada(Long id, String titulo, String tipoDescuento, BigDecimal valorDescuento, String moneda) {}
    public record LineaCotizacionCita(Long servicioId, String nombre, BigDecimal precioLista,
            BigDecimal descuento, BigDecimal precioNeto, BigDecimal anticipo, boolean promocionAplicada,
            String politicaCobro,BigDecimal importeRequerido,BigDecimal saldoEnNegocio) {
        public LineaCotizacionCita(Long servicioId,String nombre,BigDecimal precioLista,BigDecimal descuento,BigDecimal precioNeto,BigDecimal anticipo,boolean promocionAplicada) { this(servicioId,nombre,precioLista,descuento,precioNeto,anticipo,promocionAplicada,"SIN_COBRO",anticipo,precioNeto.subtract(anticipo)); }
    }
}
