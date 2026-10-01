package com.techprotech.agenda.modulos.pagos.infraestructura.checkout;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aplicacion.pagos.checkout")
public record PropiedadesCheckout(int minutosVigenciaOrden, int diasVigenciaPagoDiferido) {
    public PropiedadesCheckout {
        if (minutosVigenciaOrden < 1) minutosVigenciaOrden = 15;
        if (diasVigenciaPagoDiferido < 1 || diasVigenciaPagoDiferido > 30) diasVigenciaPagoDiferido = 3;
    }
}
