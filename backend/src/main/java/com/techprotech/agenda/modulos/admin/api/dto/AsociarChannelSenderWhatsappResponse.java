package com.techprotech.agenda.modulos.admin.api.dto;

public record AsociarChannelSenderWhatsappResponse(
        boolean asociado,
        String mensaje,
        String messagingServiceSid,
        String channelSenderSid,
        ConfiguracionWhatsappAdminResponse configuracion
) {
}
