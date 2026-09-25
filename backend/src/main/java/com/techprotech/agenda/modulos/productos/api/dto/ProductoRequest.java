package com.techprotech.agenda.modulos.productos.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductoRequest(
        Long categoriaId,
        Long proveedorId,
        Long mediaId,
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 80) String codigoBarras,
        @NotBlank @Size(max = 180) String nombre,
        @Size(max = 200) String slug,
        @Size(max = 1500) String descripcion,
        @Size(max = 1000) String imagenUrl,
        @NotNull @DecimalMin("0.0") BigDecimal precioBase,
        @DecimalMin("0.0") BigDecimal costo,
        @NotBlank @Size(max = 10) String moneda,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal tasaImpuesto,
        boolean controlaInventario,
        @NotNull @DecimalMin("0.0") BigDecimal stockMinimo,
        boolean visiblePublico,
        boolean activo,
        List<@Valid PrecioZonaProductoRequest> preciosZona
) {}
