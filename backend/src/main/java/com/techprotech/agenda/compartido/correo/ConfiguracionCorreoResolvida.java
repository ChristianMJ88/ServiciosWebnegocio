package com.techprotech.agenda.compartido.correo;

public record ConfiguracionCorreoResolvida(
        Long empresaId,
        boolean habilitado,
        ProveedorCorreo proveedor,
        String remitente,
        String nombreRemitente,
        String responderA,
        String smtpHost,
        int smtpPort,
        String smtpUsername,
        String smtpPassword,
        boolean smtpAuth,
        boolean smtpStartTls,
        String graphTenantId,
        String graphClientId,
        String graphClientSecret,
        String graphUserId,
        String graphCertificateThumbprint,
        String graphPrivateKeyPem,
        String graphOauthRefreshToken
) {
    public ConfiguracionCorreoResolvida(
            boolean habilitado,
            ProveedorCorreo proveedor,
            String remitente,
            String nombreRemitente,
            String responderA,
            String smtpHost,
            int smtpPort,
            String smtpUsername,
            String smtpPassword,
            boolean smtpAuth,
            boolean smtpStartTls,
            String graphTenantId,
            String graphClientId,
            String graphClientSecret,
            String graphUserId,
            String graphCertificateThumbprint,
            String graphPrivateKeyPem
    ) {
        this(null, habilitado, proveedor, remitente, nombreRemitente, responderA, smtpHost, smtpPort,
                smtpUsername, smtpPassword, smtpAuth, smtpStartTls, graphTenantId, graphClientId,
                graphClientSecret, graphUserId, graphCertificateThumbprint, graphPrivateKeyPem, null);
    }
}
