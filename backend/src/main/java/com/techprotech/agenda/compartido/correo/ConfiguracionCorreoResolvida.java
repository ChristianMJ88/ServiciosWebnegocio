package com.techprotech.agenda.compartido.correo;

public record ConfiguracionCorreoResolvida(
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
}
