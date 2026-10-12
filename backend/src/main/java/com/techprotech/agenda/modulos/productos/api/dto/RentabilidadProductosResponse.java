package com.techprotech.agenda.modulos.productos.api.dto;
import java.math.BigDecimal;
import java.util.List;
public record RentabilidadProductosResponse(List<Fila> filas, int pagina, int totalPaginas, long totalElementos, String alcance) {
    public record Fila(Long productoId, String sku, String nombre, String categoria, String moneda,
                       BigDecimal precio, BigDecimal costo, BigDecimal diferencia, BigDecimal margenPorcentaje) {}
}
