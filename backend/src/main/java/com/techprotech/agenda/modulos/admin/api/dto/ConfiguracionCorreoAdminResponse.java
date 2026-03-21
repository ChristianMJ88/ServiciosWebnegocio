package com.techprotech.agenda.modulos.admin.api.dto;

public record ConfiguracionCorreoAdminResponse(
        boolean habilitado,
        String proveedor,
        String remitente,
        String nombreRemitente,
        String responderA,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        boolean smtpPasswordConfigurada,
        boolean smtpPasswordCifrada,
        boolean requiereMigracionSecretos,
        Boolean smtpAuth,
        Boolean smtpStartTls,
        String graphTenantId,
        String graphClientId,
        String graphUserId,
        boolean graphClientSecretConfigurado,
        boolean graphClientSecretCifrado,
        String graphCertificateThumbprint,
        boolean graphPrivateKeyConfigurada,
        boolean graphPrivateKeyCifrada
) {
}
