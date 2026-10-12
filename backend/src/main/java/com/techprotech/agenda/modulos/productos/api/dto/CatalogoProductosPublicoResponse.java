package com.techprotech.agenda.modulos.productos.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CatalogoProductosPublicoResponse(
        Long empresaId,
        String slug,
        String nombreComercial,
        ConfiguracionTiendaResponse tienda,
        ZonaPublica zonaSeleccionada,
        List<ZonaPublica> zonas,
        List<CategoriaPublica> categorias,
        List<ProductoPublico> productos, Long sucursalSeleccionadaId, List<SucursalPublica> sucursales
) {
    public record SucursalPublica(Long id,String nombre) {}
    public record ZonaPublica(Long id, String nombre, String clave) {}
    public record CategoriaPublica(Long id, Long categoriaPadreId, String nombre, String slug, String descripcion) {}
    public record ProductoPublico(
            Long id,
            Long categoriaId,
            String categoriaNombre,
            String nombre,
            String slug,
            String descripcion,
            String imagenUrl,
            BigDecimal precio,
            BigDecimal precioAnterior,
            String moneda,
            String descuento,
            boolean controlaInventario,
            boolean disponible,
            BigDecimal existencia, boolean nuevo
    ) {}
}
