package com.techprotech.agenda.modulos.productos.api.dto;

import java.util.List;

public record IndiceProductosPublicosResponse(List<ProductoIndexado> productos) {
    public record ProductoIndexado(String sitioSlug, String productoSlug) {}
}
