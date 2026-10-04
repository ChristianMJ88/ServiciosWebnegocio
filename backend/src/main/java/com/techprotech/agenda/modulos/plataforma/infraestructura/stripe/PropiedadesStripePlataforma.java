package com.techprotech.agenda.modulos.plataforma.infraestructura.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.plataforma.stripe")
public record PropiedadesStripePlataforma(
        boolean habilitado,
        String ambiente,
        String secretKey,
        String webhookSecret,
        String webhookPublicBaseUrl,
        String checkoutSuccessUrl,
        String checkoutCancelUrl,
        String portalReturnUrl
) {}
