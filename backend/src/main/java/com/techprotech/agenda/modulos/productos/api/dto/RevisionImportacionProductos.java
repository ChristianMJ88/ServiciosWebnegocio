package com.techprotech.agenda.modulos.productos.api.dto;
import java.util.List;
public record RevisionImportacionProductos(List<Fila> filas, int total, boolean puedeImportar) {
    public record Fila(int fila, String sku, String nombre, List<String> errores) {}
}
