package com.techprotech.agenda.modulos.envios.infraestructura.envia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.envios.envia")
public record PropiedadesEnvia(
        String geocodigosBaseUrl,
        String apiPruebasBaseUrl,
        String apiProduccionBaseUrl,
        String consultasPruebasBaseUrl,
        String consultasProduccionBaseUrl,
        String registroPruebasUrl,
        String credencialesPruebasUrl,
        String registroProduccionUrl,
        String credencialesProduccionUrl,
        boolean plataformaHabilitada,
        String tokenPlataformaPruebas,
        String tokenPlataformaProduccion
) {}
