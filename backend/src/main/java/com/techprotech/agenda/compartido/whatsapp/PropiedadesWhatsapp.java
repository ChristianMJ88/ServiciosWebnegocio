package com.techprotech.agenda.compartido.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.whatsapp")
public record PropiedadesWhatsapp(
        boolean habilitado,
        Long empresaIdPorDefecto,
        String accountSid,
        String authToken,
        String numeroRemitente,
        String messagingServiceSid,
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
        int loteMaximoOutbox,
        long retrasoReintentoSegundos,
        long recordatorioHorasAntes,
        long ventanaRecordatorioMinutos,
        long liberacionSinConfirmacionHorasAntes
) {
}
