package com.techprotech.agenda.modulos.caja.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CitaPorCobrarResponse(
        Long citaId,
        String clienteNombre,
        String clienteTelefono,
        String servicioNombre,
        String sucursalNombre,
        LocalDateTime inicio,
        BigDecimal total,
        BigDecimal pagado,
        BigDecimal pendiente,
        String moneda,
        String estadoCita,
        String estadoPago
) {
}
