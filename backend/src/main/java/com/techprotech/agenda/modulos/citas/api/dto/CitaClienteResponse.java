package com.techprotech.agenda.modulos.citas.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CitaClienteResponse(
        Long id,
        String estado,
        Long sucursalId,
        Long servicioId,
        Long prestadorId,
        String sucursalNombre,
        String servicioNombre,
        String prestadorNombre,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        BigDecimal precio,
        String moneda,
        String notas,
        boolean cancelable
) {
}
