package com.techprotech.agenda.compartido.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracion_whatsapp_empresa")
public class ConfiguracionWhatsappEmpresaEntidad {

    @Id
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(nullable = false)
    private boolean habilitado;

    @Column(name = "account_sid", length = 80)
    private String accountSid;

    @Column(name = "auth_token", length = 255)
    private String authToken;

    @Column(name = "tipo_cuenta_twilio", length = 30)
    private String tipoCuentaTwilio;

    @Column(name = "subaccount_sid", length = 80)
    private String subaccountSid;

    @Column(name = "numero_remitente", length = 40)
    private String numeroRemitente;

    @Column(name = "messaging_service_sid", length = 80)
    private String messagingServiceSid;

    @Column(name = "channel_sender_sid", length = 80)
    private String channelSenderSid;

    @Column(name = "status_callback_url", length = 255)
    private String statusCallbackUrl;

    @Column(name = "plantilla_solicitud_confirmacion_sid", length = 80)
    private String plantillaSolicitudConfirmacionSid;

    @Column(name = "plantilla_reprogramada_pendiente_sid", length = 80)
    private String plantillaReprogramadaPendienteSid;

    @Column(name = "plantilla_recordatorio_confirmacion_sid", length = 80)
    private String plantillaRecordatorioConfirmacionSid;

    @Column(name = "plantilla_cita_confirmada_sid", length = 80)
    private String plantillaCitaConfirmadaSid;

    @Column(name = "plantilla_recordatorio_sid", length = 80)
    private String plantillaRecordatorioSid;

    @Column(name = "plantilla_cancelacion_sid", length = 80)
    private String plantillaCancelacionSid;

    @Column(name = "plantilla_liberada_sin_confirmacion_sid", length = 80)
    private String plantillaLiberadaSinConfirmacionSid;

    @Column(name = "plantilla_gracias_visita_sid", length = 80)
    private String plantillaGraciasVisitaSid;

    @Column(name = "plantilla_recordatorio_regreso_sid", length = 80)
    private String plantillaRecordatorioRegresoSid;

    @Column(name = "sender_display_name", length = 150)
    private String senderDisplayName;

    @Column(name = "sender_phone_number", length = 40)
    private String senderPhoneNumber;

    @Column(name = "sender_status", length = 40)
    private String senderStatus;

    @Column(name = "quality_rating", length = 40)
    private String qualityRating;

    @Column(name = "throughput_mps")
    private Integer throughputMps;

    @Column(name = "waba_id", length = 100)
    private String wabaId;

    @Column(name = "meta_business_manager_id", length = 100)
    private String metaBusinessManagerId;

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getTipoCuentaTwilio() {
        return tipoCuentaTwilio;
    }

    public void setTipoCuentaTwilio(String tipoCuentaTwilio) {
        this.tipoCuentaTwilio = tipoCuentaTwilio;
    }

    public String getSubaccountSid() {
        return subaccountSid;
    }

    public void setSubaccountSid(String subaccountSid) {
        this.subaccountSid = subaccountSid;
    }

    public String getNumeroRemitente() {
        return numeroRemitente;
    }

    public void setNumeroRemitente(String numeroRemitente) {
        this.numeroRemitente = numeroRemitente;
    }

    public String getMessagingServiceSid() {
        return messagingServiceSid;
    }

    public void setMessagingServiceSid(String messagingServiceSid) {
        this.messagingServiceSid = messagingServiceSid;
    }

    public String getChannelSenderSid() {
        return channelSenderSid;
    }

    public void setChannelSenderSid(String channelSenderSid) {
        this.channelSenderSid = channelSenderSid;
    }

    public String getStatusCallbackUrl() {
        return statusCallbackUrl;
    }

    public void setStatusCallbackUrl(String statusCallbackUrl) {
        this.statusCallbackUrl = statusCallbackUrl;
    }

    public String getPlantillaCitaConfirmadaSid() {
        return plantillaCitaConfirmadaSid;
    }

    public String getPlantillaSolicitudConfirmacionSid() {
        return plantillaSolicitudConfirmacionSid;
    }

    public void setPlantillaSolicitudConfirmacionSid(String plantillaSolicitudConfirmacionSid) {
        this.plantillaSolicitudConfirmacionSid = plantillaSolicitudConfirmacionSid;
    }

    public String getPlantillaReprogramadaPendienteSid() {
        return plantillaReprogramadaPendienteSid;
    }

    public void setPlantillaReprogramadaPendienteSid(String plantillaReprogramadaPendienteSid) {
        this.plantillaReprogramadaPendienteSid = plantillaReprogramadaPendienteSid;
    }

    public String getPlantillaRecordatorioConfirmacionSid() {
        return plantillaRecordatorioConfirmacionSid;
    }

    public void setPlantillaRecordatorioConfirmacionSid(String plantillaRecordatorioConfirmacionSid) {
        this.plantillaRecordatorioConfirmacionSid = plantillaRecordatorioConfirmacionSid;
    }

    public void setPlantillaCitaConfirmadaSid(String plantillaCitaConfirmadaSid) {
        this.plantillaCitaConfirmadaSid = plantillaCitaConfirmadaSid;
    }

    public String getPlantillaRecordatorioSid() {
        return plantillaRecordatorioSid;
    }

    public void setPlantillaRecordatorioSid(String plantillaRecordatorioSid) {
        this.plantillaRecordatorioSid = plantillaRecordatorioSid;
    }

    public String getPlantillaCancelacionSid() {
        return plantillaCancelacionSid;
    }

    public void setPlantillaCancelacionSid(String plantillaCancelacionSid) {
        this.plantillaCancelacionSid = plantillaCancelacionSid;
    }

    public String getPlantillaLiberadaSinConfirmacionSid() {
        return plantillaLiberadaSinConfirmacionSid;
    }

    public void setPlantillaLiberadaSinConfirmacionSid(String plantillaLiberadaSinConfirmacionSid) {
        this.plantillaLiberadaSinConfirmacionSid = plantillaLiberadaSinConfirmacionSid;
    }

    public String getPlantillaGraciasVisitaSid() {
        return plantillaGraciasVisitaSid;
    }

    public void setPlantillaGraciasVisitaSid(String plantillaGraciasVisitaSid) {
        this.plantillaGraciasVisitaSid = plantillaGraciasVisitaSid;
    }

    public String getPlantillaRecordatorioRegresoSid() {
        return plantillaRecordatorioRegresoSid;
    }

    public void setPlantillaRecordatorioRegresoSid(String plantillaRecordatorioRegresoSid) {
        this.plantillaRecordatorioRegresoSid = plantillaRecordatorioRegresoSid;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public void setSenderPhoneNumber(String senderPhoneNumber) {
        this.senderPhoneNumber = senderPhoneNumber;
    }

    public String getSenderStatus() {
        return senderStatus;
    }

    public void setSenderStatus(String senderStatus) {
        this.senderStatus = senderStatus;
    }

    public String getQualityRating() {
        return qualityRating;
    }

    public void setQualityRating(String qualityRating) {
        this.qualityRating = qualityRating;
    }

    public Integer getThroughputMps() {
        return throughputMps;
    }

    public void setThroughputMps(Integer throughputMps) {
        this.throughputMps = throughputMps;
    }

    public String getWabaId() {
        return wabaId;
    }

    public void setWabaId(String wabaId) {
        this.wabaId = wabaId;
    }

    public String getMetaBusinessManagerId() {
        return metaBusinessManagerId;
    }

    public void setMetaBusinessManagerId(String metaBusinessManagerId) {
        this.metaBusinessManagerId = metaBusinessManagerId;
    }
}
