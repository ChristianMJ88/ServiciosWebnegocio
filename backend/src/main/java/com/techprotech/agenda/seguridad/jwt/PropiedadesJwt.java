package com.techprotech.agenda.seguridad.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.jwt")
public record PropiedadesJwt(
        String secreto,
        String issuer,
        long minutosAcceso,
        long diasRefresh
) {
}

