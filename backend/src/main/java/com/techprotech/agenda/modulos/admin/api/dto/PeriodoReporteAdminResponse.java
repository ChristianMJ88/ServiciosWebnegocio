package com.techprotech.agenda.modulos.admin.api.dto;

import java.time.LocalDate;

public record PeriodoReporteAdminResponse(
        String codigo,
        String etiqueta,
        LocalDate desde,
        LocalDate hasta,
        boolean predeterminado
) {
}
