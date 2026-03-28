package com.techprotech.agenda.compartido.whatsapp;

public record ConfiguracionWhatsappResolvida(
        boolean habilitado,
        String accountSid,
        String authToken,
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
