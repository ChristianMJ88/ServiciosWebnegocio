package com.techprotech.agenda.modulos.envios.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ConfiguracionEnvioRequest(
        boolean habilitado,
        @NotBlank @Pattern(regexp = "PRUEBA|PRODUCCION") String ambiente,
        @NotBlank @Pattern(regexp = "CUENTA_PROPIA|PLATAFORMA") String modoCredencial,
        @Size(max = 2000) String token,
        boolean eliminarToken,
        boolean generarGuiaAutomaticamente,
        boolean permitirSeleccionPaquetera,
        @NotBlank @Pattern(regexp = "NINGUNO|PORCENTAJE|IMPORTE") String tipoRecargo,
        @NotNull @DecimalMin("0.0") BigDecimal valorRecargo,
        @NotNull List<@Valid OrigenEnvioRequest> origenes
) {}
