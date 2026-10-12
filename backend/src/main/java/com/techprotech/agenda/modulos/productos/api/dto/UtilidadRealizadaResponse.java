package com.techprotech.agenda.modulos.productos.api.dto;
import java.math.BigDecimal;
import java.util.List;
public record UtilidadRealizadaResponse(List<Fila> filas, int pagina, int totalPaginas, long totalElementos, String alcance) {
    public record Fila(Long productoId, String nombre, String sku, String moneda, BigDecimal cantidad,
        BigDecimal ventasNetas, BigDecimal costoHistorico, BigDecimal utilidadBruta, BigDecimal margenPorcentaje,
        long lineasSinCosto) {}
}
