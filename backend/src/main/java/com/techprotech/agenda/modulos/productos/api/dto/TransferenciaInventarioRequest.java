package com.techprotech.agenda.modulos.productos.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransferenciaInventarioRequest(
        @NotNull Long productoId,
        @NotNull Long sucursalOrigenId,
        @NotNull Long sucursalDestinoId,
        @NotNull @DecimalMin(value = "0.001") BigDecimal cantidad,
        @Size(max = 120) String referencia,
        @Size(max = 500) String notas
) {}
