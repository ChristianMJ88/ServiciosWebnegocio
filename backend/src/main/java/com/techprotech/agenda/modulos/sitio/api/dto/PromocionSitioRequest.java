package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PromocionSitioRequest(
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 500) String descripcion,
        @Size(max = 500) String imagenUrl,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        @NotBlank @Size(max = 20) String tipoDescuento,
        @NotNull @DecimalMin("0.01") BigDecimal valorDescuento,
        @NotBlank @Size(max = 10) String moneda,
        @Size(max = 40) String textoBoton,
        boolean activa,
        @Min(0) int orden,
        List<Long> servicioIds,
        List<Long> sucursalIds
) {}
