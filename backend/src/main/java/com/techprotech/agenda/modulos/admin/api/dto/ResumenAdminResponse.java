package com.techprotech.agenda.modulos.admin.api.dto;

import java.math.BigDecimal;

public record ResumenAdminResponse(
        long totalCitas,
        long pendientes,
        long confirmadas,
        long finalizadas,
        long canceladas,
        long noAsistio,
        long citasHoy,
        BigDecimal ingresosProgramados,
        BigDecimal ingresosFinalizados
) {
}
