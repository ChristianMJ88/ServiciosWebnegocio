package com.techprotech.agenda.modulos.plataforma.api.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record AsignarSuscripcionPlataformaRequest(
        @NotNull Long empresaId,
        @NotNull Long planId,
        @NotBlank @Pattern(regexp="MENSUAL|ANUAL") String ciclo,
        @DecimalMin("0.00") BigDecimal precioAcordado,
        @Size(max=10) String moneda,
        @NotBlank @Pattern(regexp="ACTIVA|PRUEBA") String estado,
        @NotNull LocalDateTime iniciaEn,
        LocalDateTime terminaEn,
        LocalDateTime pruebaHasta) {}
