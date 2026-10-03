package com.techprotech.agenda.modulos.plataforma.api.dto;

import com.techprotech.agenda.modulos.pagos.dominio.TipoComisionPlataforma;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SimularComisionPlataformaRequest(
        @NotNull TipoComisionPlataforma tipo,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal valor,
        @DecimalMin("0.00") BigDecimal importeMinimo,
        @DecimalMin("0.00") BigDecimal importeMaximo,
        @NotNull @DecimalMin(value = "0.01") BigDecimal importe,
        @NotBlank @Size(max = 10) String moneda) {}
