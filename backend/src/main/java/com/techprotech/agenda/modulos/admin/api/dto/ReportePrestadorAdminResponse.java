package com.techprotech.agenda.modulos.admin.api.dto;

import java.math.BigDecimal;

public record ReportePrestadorAdminResponse(
        Long prestadorId,
        String prestadorNombre,
        long totalCitas,
        long pendientes,
        long confirmadas,
        long finalizadas,
        long canceladas,
        long noAsistio,
        BigDecimal ingresosProgramados,
        BigDecimal ingresosFinalizados,
        BigDecimal ticketPromedio
) {
}
