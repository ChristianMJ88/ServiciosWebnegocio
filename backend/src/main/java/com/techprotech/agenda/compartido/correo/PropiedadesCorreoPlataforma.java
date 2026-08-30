package com.techprotech.agenda.compartido.correo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.correo.plataforma")
public record PropiedadesCorreoPlataforma(
        boolean habilitado,
        String sendgridApiKey,
        String remitente,
        String nombreRemitente,
        String responderA
) {
}
