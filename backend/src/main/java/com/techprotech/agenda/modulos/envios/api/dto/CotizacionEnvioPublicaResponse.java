package com.techprotech.agenda.modulos.envios.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CotizacionEnvioPublicaResponse(List<Opcion> opciones) {
    public record Opcion(
            String id,
            String paqueteria,
            String paqueteriaNombre,
            String servicio,
            String servicioNombre,
            String entregaEstimada,
            BigDecimal costo,
            String moneda,
            LocalDateTime expiraEn
    ) {}
}
