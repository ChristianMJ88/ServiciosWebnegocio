package com.techprotech.agenda.modulos.admin.api.dto;

import java.time.LocalDateTime;

public record MensajeWhatsappAdminResponse(
        Long id,
        String telefono,
        String direccion,
        String cuerpo,
        String contentSid,
        String proveedorMensajeId,
        String estado,
        String codigoErrorProveedor,
        String detalleErrorProveedor,
        LocalDateTime creadoEn
) {
}
