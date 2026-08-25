package com.techprotech.agenda.compartido.correo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

    @Column(name = "smtp_password", length = 90)
    private String smtpPassword;

    @Column(name = "smtp_auth")
    private Boolean smtpAuth;

    @Column(name = "smtp_starttls")
    private Boolean smtpStartTls;

    @Column(name = "graph_tenant_id", length = 100)
    private String graphTenantId;

    @Column(name = "graph_client_id", length = 100)
    private String graphClientId;

    @Column(name = "graph_client_secret", length = 90)
    private String graphClientSecret;

    @Column(name = "graph_user_id", length = 150)
    private String graphUserId;

    @Column(name = "graph_certificate_thumbprint", length = 100)
    private String graphCertificateThumbprint;

    @Column(name = "graph_private_key_pem", columnDefinition = "TEXT")
    private String graphPrivateKeyPem;

}
