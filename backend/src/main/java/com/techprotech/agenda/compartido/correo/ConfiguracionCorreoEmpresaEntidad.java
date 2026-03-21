package com.techprotech.agenda.compartido.correo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracion_correo_empresa")
public class ConfiguracionCorreoEmpresaEntidad {

    @Id
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(nullable = false)
    private boolean habilitado;

    @Column(length = 20)
    private String proveedor;

    @Column(length = 150)
    private String remitente;

    @Column(name = "nombre_remitente", length = 150)
    private String nombreRemitente;

    @Column(name = "responder_a", length = 150)
    private String responderA;

    @Column(name = "smtp_host", length = 150)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 150)
    private String smtpUsername;

    @Column(name = "smtp_password", length = 255)
    private String smtpPassword;

    @Column(name = "smtp_auth")
    private Boolean smtpAuth;

    @Column(name = "smtp_starttls")
    private Boolean smtpStartTls;

    @Column(name = "graph_tenant_id", length = 100)
    private String graphTenantId;

    @Column(name = "graph_client_id", length = 100)
    private String graphClientId;

    @Column(name = "graph_client_secret", length = 255)
    private String graphClientSecret;

    @Column(name = "graph_user_id", length = 150)
    private String graphUserId;

    @Column(name = "graph_certificate_thumbprint", length = 100)
    private String graphCertificateThumbprint;

    @Column(name = "graph_private_key_pem", columnDefinition = "TEXT")
    private String graphPrivateKeyPem;

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

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getNombreRemitente() {
        return nombreRemitente;
    }

    public void setNombreRemitente(String nombreRemitente) {
        this.nombreRemitente = nombreRemitente;
    }

    public String getResponderA() {
        return responderA;
    }

    public void setResponderA(String responderA) {
        this.responderA = responderA;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public Boolean getSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(Boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public Boolean getSmtpStartTls() {
        return smtpStartTls;
    }

    public void setSmtpStartTls(Boolean smtpStartTls) {
        this.smtpStartTls = smtpStartTls;
    }

    public String getGraphTenantId() {
        return graphTenantId;
    }

    public void setGraphTenantId(String graphTenantId) {
        this.graphTenantId = graphTenantId;
    }

    public String getGraphClientId() {
        return graphClientId;
    }

    public void setGraphClientId(String graphClientId) {
        this.graphClientId = graphClientId;
    }

    public String getGraphClientSecret() {
        return graphClientSecret;
    }

    public void setGraphClientSecret(String graphClientSecret) {
        this.graphClientSecret = graphClientSecret;
    }

    public String getGraphUserId() {
        return graphUserId;
    }

    public void setGraphUserId(String graphUserId) {
        this.graphUserId = graphUserId;
    }

    public String getGraphCertificateThumbprint() {
        return graphCertificateThumbprint;
    }

    public void setGraphCertificateThumbprint(String graphCertificateThumbprint) {
        this.graphCertificateThumbprint = graphCertificateThumbprint;
    }

    public String getGraphPrivateKeyPem() {
        return graphPrivateKeyPem;
    }

    public void setGraphPrivateKeyPem(String graphPrivateKeyPem) {
        this.graphPrivateKeyPem = graphPrivateKeyPem;
    }
}
