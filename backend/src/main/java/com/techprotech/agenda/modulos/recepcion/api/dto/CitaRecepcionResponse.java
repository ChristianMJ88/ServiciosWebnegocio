package com.techprotech.agenda.modulos.recepcion.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record CitaRecepcionResponse(
        Long id,
        String estado,
        Long sucursalId,
        Long servicioId,
        Long prestadorId,
        String sucursalNombre,
        String servicioNombre,
        String prestadorNombre,
        String clienteNombre,
        String clienteCorreo,
        String clienteTelefono,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        BigDecimal precio,
        String moneda,
        String notas,
        LocalDateTime checkInEn,
        Long checkInPorUsuarioId
) {
}
