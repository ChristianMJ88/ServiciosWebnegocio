package com.techprotech.agenda.modulos.admin.api.dto;

public record ConfiguracionWhatsappAdminResponse(
        boolean habilitado,
        String accountSid,
        boolean authTokenConfigurado,
        String tipoCuentaTwilio,
        String subaccountSid,
        String numeroRemitente,
        String messagingServiceSid,
        String channelSenderSid,
        String statusCallbackUrl,
        String plantillaSolicitudConfirmacionSid,
        String plantillaReprogramadaPendienteSid,
        String plantillaRecordatorioConfirmacionSid,
        String plantillaCitaConfirmadaSid,
        String plantillaRecordatorioSid,
        String plantillaCancelacionSid,
        String plantillaLiberadaSinConfirmacionSid,
        String plantillaGraciasVisitaSid,
        String plantillaRecordatorioRegresoSid,
        String senderDisplayName,
        String senderPhoneNumber,
        String senderStatus,
        String qualityRating,
        Integer throughputMps,
        String wabaId,
        String metaBusinessManagerId
) {
}
