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
    @com.fasterxml.jackson.annotation.JsonProperty("notificarEnAgenda")
    public boolean notificarEnAgenda() { return estado != null && java.util.Set.of("PENDIENTE","CONFIRMADA").contains(estado); }
    @com.fasterxml.jackson.annotation.JsonProperty("estadoEtiqueta")
    public String estadoEtiqueta() { return com.techprotech.agenda.modulos.citas.aplicacion.EtiquetasEstadoCita.de(estado); }

}
