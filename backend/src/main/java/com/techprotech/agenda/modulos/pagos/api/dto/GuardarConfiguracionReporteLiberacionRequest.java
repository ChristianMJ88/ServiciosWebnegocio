package com.techprotech.agenda.modulos.pagos.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuardarConfiguracionReporteLiberacionRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9_-]+") String prefijo,
        @NotBlank @Size(max = 40) String zonaHoraria,
        @NotBlank @Pattern(regexp = "daily|weekly|monthly") String frecuenciaTipo,
        @Min(0) @Max(23) int frecuenciaHora,
        @NotBlank @Size(max = 10) String frecuenciaValor,
        boolean incluirRetirosAlFinal,
        boolean ejecutarDespuesDeRetiro) {}
