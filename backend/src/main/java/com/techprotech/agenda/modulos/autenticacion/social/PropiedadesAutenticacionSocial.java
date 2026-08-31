package com.techprotech.agenda.modulos.autenticacion.social;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.autenticacion-social")
public record PropiedadesAutenticacionSocial(
        boolean googleHabilitado,
        String googleClientId,
        String googleClientSecret,
        String googleRedirectUri,
        String frontendRegistroUrl
) {
}
