package com.techprotech.agenda.modulos.caja.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record RegistrarVentaRequest(
        @NotNull Long sucursalId,
        Long citaId,
        Long clienteId,
        @Valid @Size(max = 100) List<ProductoVentaRequest> productos,
        @NotBlank String metodoPago,
        @Size(max = 120) String referencia,
        @Size(max = 500) String observaciones
) {
    public record ProductoVentaRequest(
            @NotNull Long productoId,
            @NotNull @DecimalMin("0.001") BigDecimal cantidad
    ) {}
}
