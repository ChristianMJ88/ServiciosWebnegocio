package com.techprotech.agenda.modulos.pagos.api.dto;

import java.time.LocalDateTime;

public record ReporteLiberacionResponse(Long id, String estado, String tareaExternaId, String reporteExternoId,
                                        String nombreArchivo, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                        int filasProcesadas, String errorUltimo, LocalDateTime solicitadoEn,
                                        LocalDateTime procesadoEn) {}
