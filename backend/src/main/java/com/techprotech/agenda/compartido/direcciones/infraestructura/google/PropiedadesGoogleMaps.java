package com.techprotech.agenda.compartido.direcciones.infraestructura.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "aplicacion.direcciones.google-maps")
public record PropiedadesGoogleMaps(
        boolean habilitado,
        boolean validacionRequerida,
        String browserApiKey,
        String serverApiKey,
        String addressValidationBaseUrl,
        String regionPredeterminada,
        List<String> regionesPermitidas
) {
    public boolean servidorDisponible() {
        return habilitado && serverApiKey != null && !serverApiKey.isBlank();
    }

    public boolean autocompleteDisponible() {
        return habilitado && browserApiKey != null && !browserApiKey.isBlank();
    }
}
