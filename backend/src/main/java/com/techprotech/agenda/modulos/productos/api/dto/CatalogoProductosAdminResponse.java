package com.techprotech.agenda.modulos.productos.api.dto;

import java.util.List;

public record CatalogoProductosAdminResponse(
        String monedaPredeterminada,
        List<CategoriaProductoResponse> categorias,
        List<ProveedorProductoResponse> proveedores,
        List<ZonaPrecioProductoResponse> zonasPrecio,
        List<ProductoResponse> productos,
        int pagina, int tamano, long totalElementos, int totalPaginas, long totalActivos, long totalPublicados, long totalProductos
) {}
