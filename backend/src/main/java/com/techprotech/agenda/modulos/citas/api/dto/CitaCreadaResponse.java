package com.techprotech.agenda.modulos.citas.api.dto;

import java.time.OffsetDateTime;

public record CitaCreadaResponse(
        Long id,
        String estado,
        Long empresaId,
        Long sucursalId,
        Long servicioId,
        Long prestadorId,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        String mensaje,
        boolean correoConfirmacionProgramado,
        boolean correoConfirmacionEnviado
) {
}
