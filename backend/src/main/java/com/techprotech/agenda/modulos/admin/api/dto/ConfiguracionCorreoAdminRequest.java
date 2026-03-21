package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ConfiguracionCorreoAdminRequest(
        boolean habilitado,
        @Size(max = 20) String proveedor,
        @Email @Size(max = 150) String remitente,
        @Size(max = 150) String nombreRemitente,
        @Email @Size(max = 150) String responderA,
        @Size(max = 150) String smtpHost,
        @Min(1) @Max(65535) Integer smtpPort,
        @Size(max = 150) String smtpUsername,
        @Size(max = 255) String smtpPassword,
        Boolean smtpAuth,
        Boolean smtpStartTls,
        @Size(max = 100) String graphTenantId,
        @Size(max = 100) String graphClientId,
        @Size(max = 255) String graphClientSecret,
        @Size(max = 150) String graphUserId,
        @Size(max = 100) String graphCertificateThumbprint,
        String graphPrivateKeyPem
) {
}
