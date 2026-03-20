package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicioAdminRequest(
        @NotNull Long sucursalId,
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 500) String descripcion,
        @Min(5) int duracionMinutos,
        @Min(0) int bufferAntesMinutos,
        @Min(0) int bufferDespuesMinutos,
        @NotNull @DecimalMin("0.0")
        BigDecimal precio,
        @NotBlank @Size(max = 10) String moneda,
        boolean activo
) {
}
