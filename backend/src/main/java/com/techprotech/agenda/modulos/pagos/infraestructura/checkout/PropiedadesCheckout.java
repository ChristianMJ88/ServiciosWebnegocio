package com.techprotech.agenda.modulos.pagos.infraestructura.checkout;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.pagos.checkout")
public record PropiedadesCheckout(int minutosVigenciaOrden) {
    public PropiedadesCheckout {
        if (minutosVigenciaOrden < 1) minutosVigenciaOrden = 15;
    }
}
