package com.techprotech.agenda.modulos.admin.api.dto;

import java.math.BigDecimal;

public record ReporteServicioAdminResponse(
        Long servicioId,
        String servicioNombre,
        long totalCitas,
        long pendientes,
        long confirmadas,
        long finalizadas,
        BigDecimal ingresosProgramados,
        BigDecimal ingresosFinalizados
) {
}
