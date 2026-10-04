package com.techprotech.agenda.modulos.plataforma.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record GuardarPlanPlataformaRequest(
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]+") @Size(max=80) String codigo,
        @NotBlank @Size(max=120) String nombre,
        @Size(max=500) String descripcion,
        @NotBlank @Size(max=10) String moneda,
        @NotNull @DecimalMin("0.00") BigDecimal precioMensual,
        @DecimalMin("0.00") BigDecimal precioAnual,
        boolean activo,
        boolean publico,
        boolean destacado,
        @Min(0) @Max(365) int diasPrueba,
        @Min(0) @Max(90) int diasGracia,
        LocalDateTime vigenteDesde,
        LocalDateTime vigenteHasta,
        @NotNull @Size(min=1) List<@Valid ModuloPlanRequest> modulos,
        @NotNull List<@Valid LimitePlanRequest> limites) {}
