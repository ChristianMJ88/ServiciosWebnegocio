package com.techprotech.agenda.modulos.plataforma.api.dto;

import com.techprotech.agenda.modulos.pagos.dominio.TipoComisionPlataforma;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GuardarReglaComisionPlataformaRequest(
        @NotNull Long proveedorId,
        Long empresaId,
        @NotNull TipoComisionPlataforma tipo,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal valor,
        @DecimalMin("0.00") BigDecimal importeMinimo,
        @DecimalMin("0.00") BigDecimal importeMaximo,
        @Size(max = 10) String moneda,
        int prioridad,
        @NotNull LocalDateTime vigenteDesde,
        LocalDateTime vigenteHasta,
        boolean activa) {}
