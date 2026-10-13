package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.sitio.dominio.TipoDescuentoPromocion;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalculadorPromocionServicios {
    private CalculadorPromocionServicios() {}
    public record Precio(BigDecimal precioLista, BigDecimal descuento, BigDecimal precioNeto) {}
    public static Precio calcular(BigDecimal lista, TipoDescuentoPromocion tipo, BigDecimal valor) {
        lista = lista.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = tipo == null || valor == null ? lista : tipo == TipoDescuentoPromocion.PRECIO
                ? valor : lista.multiply(BigDecimal.ONE.subtract(valor.divide(BigDecimal.valueOf(100))));
        neto = neto.max(BigDecimal.ZERO).min(lista).setScale(2, RoundingMode.HALF_UP);
        return new Precio(lista, lista.subtract(neto), neto);
    }
}
