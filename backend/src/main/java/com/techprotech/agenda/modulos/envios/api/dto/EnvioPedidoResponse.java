package com.techprotech.agenda.modulos.envios.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        String estadoRastreo,
        BigDecimal costoProveedor,
        BigDecimal costoCliente,
        BigDecimal totalOrden,
        String moneda,
        String destinatario,
        String destino,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {}
