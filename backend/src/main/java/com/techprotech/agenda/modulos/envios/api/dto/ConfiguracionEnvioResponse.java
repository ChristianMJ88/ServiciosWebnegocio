package com.techprotech.agenda.modulos.envios.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConfiguracionEnvioResponse(
        String proveedor, boolean habilitado, String ambiente, boolean tokenConfigurado,
        boolean generarGuiaAutomaticamente, boolean permitirSeleccionPaquetera,
        String tipoRecargo, BigDecimal valorRecargo, List<OrigenEnvioResponse> origenes
) {}
