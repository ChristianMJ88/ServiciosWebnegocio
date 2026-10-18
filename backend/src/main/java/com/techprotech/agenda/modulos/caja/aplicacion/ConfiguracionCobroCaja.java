package com.techprotech.agenda.modulos.caja.aplicacion;
@org.springframework.stereotype.Component
@org.springframework.boot.context.properties.ConfigurationProperties(prefix="fluora.caja.cobro")
@lombok.Getter @lombok.Setter
public class ConfiguracionCobroCaja {
    private java.util.Map<String,java.util.List<java.math.BigDecimal>> denominaciones=java.util.Map.of();
}
