package com.techprotech.agenda.compartido.whatsapp;

import com.techprotech.agenda.compartido.correo.ProtectorSecretosCorreo;
import org.springframework.stereotype.Service;

@Service
public class ServicioConfiguracionWhatsappEmpresa {

    private final ConfiguracionWhatsappEmpresaRepositorio configuracionWhatsappEmpresaRepositorio;
    private final PropiedadesWhatsapp propiedadesWhatsapp;
    private final ProtectorSecretosCorreo protectorSecretosCorreo;

    public ServicioConfiguracionWhatsappEmpresa(
            ConfiguracionWhatsappEmpresaRepositorio configuracionWhatsappEmpresaRepositorio,
            PropiedadesWhatsapp propiedadesWhatsapp,
            ProtectorSecretosCorreo protectorSecretosCorreo
    ) {
        this.configuracionWhatsappEmpresaRepositorio = configuracionWhatsappEmpresaRepositorio;
        this.propiedadesWhatsapp = propiedadesWhatsapp;
        this.protectorSecretosCorreo = protectorSecretosCorreo;
    }

    public ConfiguracionWhatsappResolvida resolver(Long empresaId) {
        if (!propiedadesWhatsapp.habilitado()) {
            return new ConfiguracionWhatsappResolvida(
                    false,
                    null,
                    null,
                    "PLATAFORMA",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return configuracionWhatsappEmpresaRepositorio.findById(empresaId)
                .map(this::mapearConfigEmpresa)
                .orElseGet(this::crearConfigPorDefecto);
    }

    private ConfiguracionWhatsappResolvida mapearConfigEmpresa(ConfiguracionWhatsappEmpresaEntidad entidad) {
        String tipoCuentaTwilio = valorOPropiedad(entidad.getTipoCuentaTwilio(), "PLATAFORMA");
        String subaccountSid = normalizarOpcional(entidad.getSubaccountSid());
        String accountSidBase = valorOPropiedad(entidad.getAccountSid(), propiedadesWhatsapp.accountSid());
        String accountSid = "SUBCUENTA".equalsIgnoreCase(tipoCuentaTwilio) && tieneTexto(subaccountSid)
                ? subaccountSid
                : accountSidBase;
        String authToken = protectorSecretosCorreo.desencriptarSiNecesario(
                valorOPropiedad(entidad.getAuthToken(), propiedadesWhatsapp.authToken()),
                "twilio_auth_token"
        );
        String numeroRemitente = valorOPropiedad(entidad.getNumeroRemitente(), propiedadesWhatsapp.numeroRemitente());
        String messagingServiceSid = valorOPropiedad(entidad.getMessagingServiceSid(), propiedadesWhatsapp.messagingServiceSid());
        String channelSenderSid = normalizarOpcional(entidad.getChannelSenderSid());
        String statusCallbackUrl = valorOPropiedad(entidad.getStatusCallbackUrl(), propiedadesWhatsapp.statusCallbackUrl());
        String plantillaSolicitudConfirmacionSid = valorOPropiedad(
                entidad.getPlantillaSolicitudConfirmacionSid(),
                propiedadesWhatsapp.plantillaSolicitudConfirmacionSid()
        );
        String plantillaReprogramadaPendienteSid = valorOPropiedad(
                entidad.getPlantillaReprogramadaPendienteSid(),
                propiedadesWhatsapp.plantillaReprogramadaPendienteSid()
        );
        String plantillaRecordatorioConfirmacionSid = valorOPropiedad(
                entidad.getPlantillaRecordatorioConfirmacionSid(),
                propiedadesWhatsapp.plantillaRecordatorioConfirmacionSid()
        );
        String plantillaCitaConfirmadaSid = valorOPropiedad(entidad.getPlantillaCitaConfirmadaSid(), propiedadesWhatsapp.plantillaCitaConfirmadaSid());
        String plantillaRecordatorioSid = valorOPropiedad(entidad.getPlantillaRecordatorioSid(), propiedadesWhatsapp.plantillaRecordatorioSid());
        String plantillaCancelacionSid = valorOPropiedad(entidad.getPlantillaCancelacionSid(), propiedadesWhatsapp.plantillaCancelacionSid());
        String plantillaLiberadaSinConfirmacionSid = valorOPropiedad(
                entidad.getPlantillaLiberadaSinConfirmacionSid(),
                propiedadesWhatsapp.plantillaLiberadaSinConfirmacionSid()
        );
        String plantillaGraciasVisitaSid = valorOPropiedad(
                entidad.getPlantillaGraciasVisitaSid(),
                propiedadesWhatsapp.plantillaGraciasVisitaSid()
        );
        String plantillaRecordatorioRegresoSid = valorOPropiedad(
                entidad.getPlantillaRecordatorioRegresoSid(),
                propiedadesWhatsapp.plantillaRecordatorioRegresoSid()
        );

        return new ConfiguracionWhatsappResolvida(
                entidad.isHabilitado() && configuracionMinimaValida(accountSid, authToken, numeroRemitente),
                accountSid,
                authToken,
                tipoCuentaTwilio,
                subaccountSid,
                numeroRemitente,
                messagingServiceSid,
                channelSenderSid,
                statusCallbackUrl,
                plantillaSolicitudConfirmacionSid,
                plantillaReprogramadaPendienteSid,
                plantillaRecordatorioConfirmacionSid,
                plantillaCitaConfirmadaSid,
                plantillaRecordatorioSid,
                plantillaCancelacionSid,
                plantillaLiberadaSinConfirmacionSid,
                plantillaGraciasVisitaSid,
                plantillaRecordatorioRegresoSid,
                entidad.getSenderDisplayName(),
                entidad.getSenderPhoneNumber(),
                entidad.getSenderStatus(),
                entidad.getQualityRating(),
                entidad.getThroughputMps(),
                entidad.getWabaId(),
                entidad.getMetaBusinessManagerId()
        );
    }

    private ConfiguracionWhatsappResolvida crearConfigPorDefecto() {
        return new ConfiguracionWhatsappResolvida(
                configuracionMinimaValida(
                        propiedadesWhatsapp.accountSid(),
                        propiedadesWhatsapp.authToken(),
                        propiedadesWhatsapp.numeroRemitente()
                ),
                propiedadesWhatsapp.accountSid(),
                propiedadesWhatsapp.authToken(),
                "PLATAFORMA",
                null,
                propiedadesWhatsapp.numeroRemitente(),
                propiedadesWhatsapp.messagingServiceSid(),
                null,
                propiedadesWhatsapp.statusCallbackUrl(),
                propiedadesWhatsapp.plantillaSolicitudConfirmacionSid(),
                propiedadesWhatsapp.plantillaReprogramadaPendienteSid(),
                propiedadesWhatsapp.plantillaRecordatorioConfirmacionSid(),
                propiedadesWhatsapp.plantillaCitaConfirmadaSid(),
                propiedadesWhatsapp.plantillaRecordatorioSid(),
                propiedadesWhatsapp.plantillaCancelacionSid(),
                propiedadesWhatsapp.plantillaLiberadaSinConfirmacionSid(),
                propiedadesWhatsapp.plantillaGraciasVisitaSid(),
                propiedadesWhatsapp.plantillaRecordatorioRegresoSid(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private boolean configuracionMinimaValida(String accountSid, String authToken, String numeroRemitente) {
        return tieneTexto(accountSid) && tieneTexto(authToken) && tieneTexto(numeroRemitente);
    }

    private String valorOPropiedad(String valorTenant, String valorDefecto) {
        if (tieneTexto(valorTenant)) {
            return valorTenant;
        }
        return valorDefecto;
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
