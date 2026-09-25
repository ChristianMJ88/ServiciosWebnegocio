package com.techprotech.agenda.modulos.productos.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AjusteInventarioRequest(
        @NotNull Long productoId,
        @NotNull Long sucursalId,
        @NotBlank String tipo,
        @NotNull @DecimalMin(value = "0.000") BigDecimal cantidad,
        @Size(max = 120) String referencia,
        @Size(max = 500) String notas
) {}
