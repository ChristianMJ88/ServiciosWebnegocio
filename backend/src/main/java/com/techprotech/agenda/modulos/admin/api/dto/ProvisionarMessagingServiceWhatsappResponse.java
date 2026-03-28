package com.techprotech.agenda.modulos.admin.api.dto;

public record ProvisionarMessagingServiceWhatsappResponse(
        boolean creado,
        String mensaje,
        String friendlyName,
        String messagingServiceSid,
        String inboundRequestUrl,
        ConfiguracionWhatsappAdminResponse configuracion
) {
}
