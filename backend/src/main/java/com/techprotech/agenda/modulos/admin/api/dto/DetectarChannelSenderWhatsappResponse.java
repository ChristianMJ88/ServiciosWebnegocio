package com.techprotech.agenda.modulos.admin.api.dto;

public record DetectarChannelSenderWhatsappResponse(
        boolean encontrado,
        String mensaje,
        String channelSenderSid,
        String senderId,
        String senderStatus,
        String displayName,
        String wabaId,
        ConfiguracionWhatsappAdminResponse configuracion
) {
}
