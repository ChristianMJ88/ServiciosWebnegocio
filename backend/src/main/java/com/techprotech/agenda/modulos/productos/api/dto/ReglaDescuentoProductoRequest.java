package com.techprotech.agenda.modulos.productos.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReglaDescuentoProductoRequest(
        @NotBlank @Size(max=160) String nombre,
        @Size(max=500) String descripcion,
        @NotBlank String alcance,
        Long productoId, Long categoriaId, Long zonaPrecioId, Long sucursalId,
        @NotBlank String tipoDescuento,
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        @Size(max=10) String moneda,
        @NotNull @DecimalMin("0.001") BigDecimal cantidadMinima,
        LocalDate fechaInicio, LocalDate fechaFin,
        int prioridad, boolean combinable, boolean activa
) {}
