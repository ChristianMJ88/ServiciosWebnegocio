package com.techprotech.agenda.modulos.productos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReglaDescuentoProductoResponse(
        Long id, String nombre, String descripcion, String alcance,
        Long productoId, String productoNombre, Long categoriaId, String categoriaNombre,
        Long zonaPrecioId, String zonaPrecioNombre, Long sucursalId, String sucursalNombre,
        String tipoDescuento, BigDecimal valor, String moneda, BigDecimal cantidadMinima,
        LocalDate fechaInicio, LocalDate fechaFin, int prioridad, boolean combinable, boolean activa, String estado
) {}
