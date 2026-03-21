package com.techprotech.agenda.compartido.correo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.correo")
public record PropiedadesCorreo(
        boolean habilitado,
        String proveedorPorDefecto,
        String remitentePorDefecto,
        String nombreRemitentePorDefecto,
        String responderAPorDefecto,
        String graphTenantId,
        String graphClientId,
        String graphClientSecret,
        String graphUserId,
        String graphCertificateThumbprint,
        String graphPrivateKeyPem,
        String llaveCifradoSecretos,
        int loteMaximoOutbox,
        long retrasoReintentoSegundos
) {
}
