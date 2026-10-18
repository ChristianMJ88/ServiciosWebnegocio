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
        com.techprotech.agenda.modulos.caja.aplicacion.EstadoCobroCita estadoCobro,
        BigDecimal precioLista,
        BigDecimal descuento,
        String promocionTitulo,
        Long sucursalId,
        String periodoCobro,
        long diasAtraso,
        String folio,
        Long clienteId,
        boolean cobroPointEnCurso
) {
    /** Alias legado; el valor procede del mismo enum, sin un segundo cálculo. */
    @Deprecated
    @com.fasterxml.jackson.annotation.JsonProperty("estadoPago")
    public String estadoPago() {
        return estadoCobro == com.techprotech.agenda.modulos.caja.aplicacion.EstadoCobroCita.PAGADA
                ? "PAGADO" : estadoCobro.name();
    }
}
