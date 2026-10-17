package com.techprotech.agenda.modulos.caja.infraestructura.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "aplicacion.caja")
public class ConfiguracionCaja {
    @Min(0) @Max(3650)
    private int diasPendientes = 90;
    public int getDiasPendientes() { return diasPendientes; }
    public void setDiasPendientes(int diasPendientes) { this.diasPendientes = diasPendientes; }
}
