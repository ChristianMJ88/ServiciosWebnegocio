package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ServicioAdminRequest(
        @NotNull Long sucursalId,
        List<Long> sucursalIds,
        Long grupoId,
        Long subgrupoId,
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 140) String slug,
        @Size(max = 500) String descripcion,
        @Size(max = 255) String imagenUrl,
        @Min(5) int duracionMinutos,
        @Min(0) int bufferAntesMinutos,
        @Min(0) int bufferDespuesMinutos,
        @NotNull @DecimalMin("0.0")
        BigDecimal precio,
        @NotBlank @Size(max = 10) String moneda,
        @Min(0) int ordenPublico,
        boolean visiblePublico,
        boolean requiereAnticipo,
        @Size(max = 20) String anticipoTipo,
        @DecimalMin("0.0") BigDecimal anticipoValor,
        boolean activo
) {
}
