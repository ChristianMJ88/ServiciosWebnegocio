package com.techprotech.agenda.modulos.admin.api.dto;

import java.time.LocalDateTime;

public record LogMensajeWhatsappAdminResponse(
        Long id,
        Long agregadoId,
        String tipoEvento,
        String estado,
        String estadoEntrega,
        String proveedorMensajeId,
        String destinatario,
        String plantillaSid,
        String codigoErrorProveedor,
        String detalleErrorProveedor,
        LocalDateTime enviadaEn,
        LocalDateTime estadoEntregaActualizadoEn
) {
}
