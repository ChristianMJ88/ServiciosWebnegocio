package com.techprotech.agenda.modulos.recepcion.api.dto;

import java.time.OffsetDateTime;

public record FranjaRecepcionDisponibleResponse(
        String inicio,
        String fin,
        String hora,
        Long prestadorId,
        Long servicioId,
        Long sucursalId,
        OffsetDateTime inicioAt,
        OffsetDateTime finAt
) {
}
