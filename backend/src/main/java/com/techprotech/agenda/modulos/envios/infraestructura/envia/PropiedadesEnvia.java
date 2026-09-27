package com.techprotech.agenda.modulos.envios.infraestructura.envia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.envios.envia")
public record PropiedadesEnvia(String geocodigosBaseUrl, String apiPruebasBaseUrl, String apiProduccionBaseUrl) {}
