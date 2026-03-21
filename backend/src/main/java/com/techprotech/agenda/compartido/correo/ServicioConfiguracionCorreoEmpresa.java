package com.techprotech.agenda.compartido.correo;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.stereotype.Service;

@Service
public class ServicioConfiguracionCorreoEmpresa {

    private final ConfiguracionCorreoEmpresaRepositorio configuracionCorreoEmpresaRepositorio;
    private final PropiedadesCorreo propiedadesCorreo;
    private final MailProperties mailProperties;
    private final ProtectorSecretosCorreo protectorSecretosCorreo;

    public ServicioConfiguracionCorreoEmpresa(
            ConfiguracionCorreoEmpresaRepositorio configuracionCorreoEmpresaRepositorio,
            PropiedadesCorreo propiedadesCorreo,
            MailProperties mailProperties,
            ProtectorSecretosCorreo protectorSecretosCorreo
    ) {
        this.configuracionCorreoEmpresaRepositorio = configuracionCorreoEmpresaRepositorio;
        this.propiedadesCorreo = propiedadesCorreo;
        this.mailProperties = mailProperties;
        this.protectorSecretosCorreo = protectorSecretosCorreo;
    }

    public ConfiguracionCorreoResolvida resolver(Long empresaId) {
        if (!propiedadesCorreo.habilitado()) {
            return new ConfiguracionCorreoResolvida(false, ProveedorCorreo.SMTP, null, null, null, null, 0, null, null, false, false, null, null, null, null, null, null);
        }

        return configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .map(this::mapearConfigEmpresa)
                .orElseGet(this::crearConfigPorDefecto);
    }

    private ConfiguracionCorreoResolvida mapearConfigEmpresa(ConfiguracionCorreoEmpresaEntidad entidad) {
        ProveedorCorreo proveedor = ProveedorCorreo.desdeValor(entidad.getProveedor(), proveedorPorDefecto());
        String remitente = valorOPropiedad(entidad.getRemitente(), propiedadesCorreo.remitentePorDefecto());
        String nombreRemitente = valorOPropiedad(entidad.getNombreRemitente(), propiedadesCorreo.nombreRemitentePorDefecto());
        String responderA = valorOPropiedad(entidad.getResponderA(), propiedadesCorreo.responderAPorDefecto());
        String smtpHost = valorOPropiedad(entidad.getSmtpHost(), mailProperties.getHost());
        int smtpPort = entidad.getSmtpPort() != null ? entidad.getSmtpPort() : mailProperties.getPort();
        String smtpUsername = valorOPropiedad(entidad.getSmtpUsername(), mailProperties.getUsername());
        String smtpPassword = protectorSecretosCorreo.desencriptarSiNecesario(
                valorOPropiedad(entidad.getSmtpPassword(), mailProperties.getPassword())
        );
        boolean smtpAuth = entidad.getSmtpAuth() != null ? entidad.getSmtpAuth() : Boolean.parseBoolean(mailProperties.getProperties().getOrDefault("mail.smtp.auth", "false"));
        boolean smtpStartTls = entidad.getSmtpStartTls() != null ? entidad.getSmtpStartTls() : Boolean.parseBoolean(mailProperties.getProperties().getOrDefault("mail.smtp.starttls.enable", "false"));
        String graphTenantId = valorOPropiedad(entidad.getGraphTenantId(), propiedadesCorreo.graphTenantId());
        String graphClientId = valorOPropiedad(entidad.getGraphClientId(), propiedadesCorreo.graphClientId());
        String graphClientSecret = protectorSecretosCorreo.desencriptarSiNecesario(
                valorOPropiedad(entidad.getGraphClientSecret(), propiedadesCorreo.graphClientSecret())
        );
        String graphUserId = valorOPropiedad(entidad.getGraphUserId(), propiedadesCorreo.graphUserId());
        String graphCertificateThumbprint = valorOPropiedad(entidad.getGraphCertificateThumbprint(), propiedadesCorreo.graphCertificateThumbprint());
        String graphPrivateKeyPem = protectorSecretosCorreo.desencriptarSiNecesario(
                valorOPropiedad(entidad.getGraphPrivateKeyPem(), propiedadesCorreo.graphPrivateKeyPem())
        );

        return new ConfiguracionCorreoResolvida(
                entidad.isHabilitado() && configuracionMinimaValida(
                        proveedor,
                        remitente,
                        smtpHost,
                        smtpPort,
                        graphTenantId,
                        graphClientId,
                        graphClientSecret,
                        graphUserId,
                        graphCertificateThumbprint,
                        graphPrivateKeyPem
                ),
                proveedor,
                remitente,
                nombreRemitente,
                responderA,
                smtpHost,
                smtpPort,
                smtpUsername,
                smtpPassword,
                smtpAuth,
                smtpStartTls,
                graphTenantId,
                graphClientId,
                graphClientSecret,
                graphUserId,
                graphCertificateThumbprint,
                graphPrivateKeyPem
        );
    }

    private ConfiguracionCorreoResolvida crearConfigPorDefecto() {
        ProveedorCorreo proveedor = proveedorPorDefecto();
        String remitente = propiedadesCorreo.remitentePorDefecto();
        String smtpHost = mailProperties.getHost();
        int smtpPort = mailProperties.getPort();
        boolean smtpAuth = Boolean.parseBoolean(mailProperties.getProperties().getOrDefault("mail.smtp.auth", "false"));
        boolean smtpStartTls = Boolean.parseBoolean(mailProperties.getProperties().getOrDefault("mail.smtp.starttls.enable", "false"));
        return new ConfiguracionCorreoResolvida(
                configuracionMinimaValida(
                        proveedor,
                        remitente,
                        smtpHost,
                        smtpPort,
                        propiedadesCorreo.graphTenantId(),
                        propiedadesCorreo.graphClientId(),
                        propiedadesCorreo.graphClientSecret(),
                        propiedadesCorreo.graphUserId(),
                        propiedadesCorreo.graphCertificateThumbprint(),
                        propiedadesCorreo.graphPrivateKeyPem()
                ),
                proveedor,
                remitente,
                propiedadesCorreo.nombreRemitentePorDefecto(),
                propiedadesCorreo.responderAPorDefecto(),
                smtpHost,
                smtpPort,
                mailProperties.getUsername(),
                mailProperties.getPassword(),
                smtpAuth,
                smtpStartTls,
                propiedadesCorreo.graphTenantId(),
                propiedadesCorreo.graphClientId(),
                propiedadesCorreo.graphClientSecret(),
                propiedadesCorreo.graphUserId(),
                propiedadesCorreo.graphCertificateThumbprint(),
                propiedadesCorreo.graphPrivateKeyPem()
        );
    }

    private boolean configuracionMinimaValida(
            ProveedorCorreo proveedor,
            String remitente,
            String smtpHost,
            int smtpPort,
            String graphTenantId,
            String graphClientId,
            String graphClientSecret,
            String graphUserId,
            String graphCertificateThumbprint,
            String graphPrivateKeyPem
    ) {
        if (remitente == null || remitente.isBlank()) {
            return false;
        }

        if (proveedor == ProveedorCorreo.GRAPH) {
            boolean tieneCertificado = graphCertificateThumbprint != null && !graphCertificateThumbprint.isBlank()
                    && graphPrivateKeyPem != null && !graphPrivateKeyPem.isBlank();
            boolean tieneSecret = graphClientSecret != null && !graphClientSecret.isBlank();
            return graphTenantId != null && !graphTenantId.isBlank()
                    && graphClientId != null && !graphClientId.isBlank()
                    && graphUserId != null && !graphUserId.isBlank()
                    && (tieneCertificado || tieneSecret);
        }

        return smtpHost != null && !smtpHost.isBlank()
                && smtpPort > 0;
    }

    private ProveedorCorreo proveedorPorDefecto() {
        return ProveedorCorreo.desdeValor(propiedadesCorreo.proveedorPorDefecto(), ProveedorCorreo.SMTP);
    }

    private String valorOPropiedad(String valorTenant, String valorDefecto) {
        if (valorTenant != null && !valorTenant.isBlank()) {
            return valorTenant;
        }
        return valorDefecto;
    }
}
