package com.techprotech.agenda.modulos.envios.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ActualizarEnvioManualRequest(
        @NotBlank @Size(max = 80) String paqueteria,
        @Size(max = 120) String servicio,
        @NotBlank @Size(max = 160) String numeroGuia,
        @Size(max = 1000) String urlEtiqueta,
        @NotNull @DecimalMin("0.0") BigDecimal costoProveedor,
        @NotBlank @Pattern(regexp = "GUIA_GENERADA|EN_TRANSITO|ENTREGADO") String estado
) {}
