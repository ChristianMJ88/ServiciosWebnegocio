package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.pagos.mercado-pago")
public record PropiedadesMercadoPago(
        boolean habilitado,
        String ambiente,
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendResultadoUrl,
        String webhookPublicBaseUrl,
        String webhookSecret,
        String publicKeyPrueba,
        String accessTokenPrueba,
        String pointTerminalPruebaId
) {}
