package com.techprotech.agenda.modulos.autenticacion.social;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.autenticacion-social")
public record PropiedadesAutenticacionSocial(
        boolean googleHabilitado,
        String googleClientId,
        String googleClientSecret,
        String googleRedirectUri,
        boolean microsoftHabilitado,
        String microsoftClientId,
        String microsoftCertificateThumbprint,
        String microsoftPrivateKeyPem,
        String microsoftRedirectUri,
        String frontendRegistroUrl,
        String frontendAccesoUrl
) {
}
