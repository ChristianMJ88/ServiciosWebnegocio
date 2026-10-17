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
        String notas,
        Long sucursalId,
        java.math.BigDecimal pagado,
        java.math.BigDecimal condonado,
        java.math.BigDecimal pendiente,
        com.techprotech.agenda.modulos.caja.aplicacion.EstadoCobroCita estadoCobro,
        boolean puedeCobrar,
        boolean puedeCerrar,
        String moneda,
        String folio
) {
    @com.fasterxml.jackson.annotation.JsonProperty("notificarEnAgenda")
    public boolean notificarEnAgenda() { return estado != null && java.util.Set.of("PENDIENTE","CONFIRMADA").contains(estado); }
    @com.fasterxml.jackson.annotation.JsonProperty("estadoEtiqueta")
    public String estadoEtiqueta() { return com.techprotech.agenda.modulos.citas.aplicacion.EtiquetasEstadoCita.de(estado); }

}

