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
        boolean cancelable,
        String clienteNombre,
        String clienteCorreo,
        String clienteTelefono,
        String folio
) {
    @com.fasterxml.jackson.annotation.JsonProperty("notificarEnAgenda")
    public boolean notificarEnAgenda() { return estado != null && java.util.Set.of("PENDIENTE","CONFIRMADA").contains(estado); }
    @com.fasterxml.jackson.annotation.JsonProperty("estadoEtiqueta")
    public String estadoEtiqueta() { return com.techprotech.agenda.modulos.citas.aplicacion.EtiquetasEstadoCita.de(estado); }

}
