package com.techprotech.agenda.modulos.prestadores.api.dto;

import java.time.OffsetDateTime;

public record CitaAgendaResponse(
        Long id,
        String estado,
        String sucursalNombre,
        String servicioNombre,
        String clienteNombre,
        String clienteCorreo,
        String clienteTelefono,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        String notas
) {
}

