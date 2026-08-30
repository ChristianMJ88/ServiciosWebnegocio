package com.techprotech.agenda.compartido.correo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.correo.google-oauth")
public record PropiedadesOAuthCorreoGoogle(
        boolean habilitado,
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendResultadoUrl
) {
}
