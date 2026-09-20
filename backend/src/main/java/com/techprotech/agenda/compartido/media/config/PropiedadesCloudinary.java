package com.techprotech.agenda.compartido.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "aplicacion.cloudinary")
public record PropiedadesCloudinary(
        boolean habilitado,
        String cloudName,
        String apiKey,
        String apiSecret,
        String carpetaRaiz,
        DataSize maxTamanoImagen
) {
    public boolean configurado() {
        return habilitado && tieneValor(cloudName) && tieneValor(apiKey) && tieneValor(apiSecret);
    }

    private boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    public long maxBytesImagen() {
        return maxTamanoImagen.toBytes();
    }
}
