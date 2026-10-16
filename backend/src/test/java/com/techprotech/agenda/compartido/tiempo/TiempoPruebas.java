package com.techprotech.agenda.compartido.tiempo;

import java.time.ZoneOffset;
import org.mockito.Mockito;

/** Mantiene los fixtures heredados en UTC; las regresiones de zonas usan datos explícitos. */
public final class TiempoPruebas {
    private TiempoPruebas() {}
    public static java.time.Clock relojUtc() { return new RelojUtc(); }
    public static java.util.Map<String,Object> configuracionJpa() {
        var yaml = new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
        yaml.setResources(new org.springframework.core.io.ClassPathResource("application.yml"));
        var configuracion = new java.util.HashMap<String,Object>();
        java.util.Objects.requireNonNull(yaml.getObject()).forEach((clave,valor) -> {
            var nombre=clave.toString();
            if (nombre.startsWith("spring.jpa.properties.")) configuracion.put(nombre.substring("spring.jpa.properties.".length()),valor);
        });
        return configuracion;
    }
    public static class RelojUtc extends java.time.Clock {
        private final java.time.Clock base = java.time.Clock.systemUTC();
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId zona) { return base.withZone(zona); }
        @Override public java.time.Instant instant() { return base.instant(); }
    }
    public static ZonaNegocio zonaUtc() {
        return zona(ZoneOffset.UTC);
    }
    public static ZonaNegocio zona(java.time.ZoneId zona) {
        return Mockito.mock(ZonaNegocio.class, invocacion -> invocacion.getMethod().getReturnType().equals(java.time.ZoneId.class)
                ? zona : org.mockito.Answers.RETURNS_DEFAULTS.answer(invocacion));
    }
}
