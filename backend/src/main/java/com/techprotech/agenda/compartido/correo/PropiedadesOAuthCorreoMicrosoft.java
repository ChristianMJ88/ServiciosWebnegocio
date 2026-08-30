package com.techprotech.agenda.compartido.correo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.correo.microsoft-oauth")
public record PropiedadesOAuthCorreoMicrosoft(
        boolean habilitado,
        String clientId,
        String certificateThumbprint,
        String privateKeyPem,
        String redirectUri,
        String frontendResultadoUrl
) {
}

