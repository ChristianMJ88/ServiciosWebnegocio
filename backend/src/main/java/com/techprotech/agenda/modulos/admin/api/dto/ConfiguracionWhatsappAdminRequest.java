package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ConfiguracionWhatsappAdminRequest(
        boolean habilitado,
        @Size(max = 80) String accountSid,
        @Size(max = 255) String authToken,
        @Size(max = 30) String tipoCuentaTwilio,
        @Size(max = 80) String subaccountSid,
        @Size(max = 40) String numeroRemitente,
        @Size(max = 80) String messagingServiceSid,
        @Size(max = 80) String channelSenderSid,
        @Size(max = 255) String statusCallbackUrl,
        @Size(max = 80) String plantillaSolicitudConfirmacionSid,
        @Size(max = 80) String plantillaReprogramadaPendienteSid,
        @Size(max = 80) String plantillaRecordatorioConfirmacionSid,
        @Size(max = 80) String plantillaCitaConfirmadaSid,
        @Size(max = 80) String plantillaRecordatorioSid,
        @Size(max = 80) String plantillaCancelacionSid,
        @Size(max = 80) String plantillaLiberadaSinConfirmacionSid,
        @Size(max = 80) String plantillaGraciasVisitaSid,
        @Size(max = 80) String plantillaRecordatorioRegresoSid,
        @Size(max = 150) String senderDisplayName,
        @Size(max = 40) String senderPhoneNumber,
        @Size(max = 40) String senderStatus,
        @Size(max = 40) String qualityRating,
        @Min(0) @Max(100000) Integer throughputMps,
        @Size(max = 100) String wabaId,
        @Size(max = 100) String metaBusinessManagerId
) {
}
