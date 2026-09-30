package com.techprotech.agenda.modulos.envios.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EnvioPedidoResponse(
        Long id,
        String ordenPublicId,
        String estado,
        String motivoBloqueo,
        String ambiente,
        String paqueteria,
        String servicio,
        String numeroGuia,
        String urlEtiqueta,
        String urlRastreo,
        String estadoRastreo,
        BigDecimal costoProveedor,
        BigDecimal costoCliente,
        BigDecimal totalOrden,
        String moneda,
        String destinatario,
        String destino,
        int intentos,
        String ultimoError,
        List<EnvioEventoResponse> eventos,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {}
