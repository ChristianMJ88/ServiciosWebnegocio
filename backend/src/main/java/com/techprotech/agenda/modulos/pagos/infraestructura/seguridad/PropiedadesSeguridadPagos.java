package com.techprotech.agenda.modulos.pagos.infraestructura.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.pagos")
public record PropiedadesSeguridadPagos(String llaveCifradoSecretos) {}
