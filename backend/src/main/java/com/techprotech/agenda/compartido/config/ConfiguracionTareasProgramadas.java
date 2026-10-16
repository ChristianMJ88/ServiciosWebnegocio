package com.techprotech.agenda.compartido.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Permite arrancar el contexto completo sin ejecutar trabajos ni integraciones externas. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "aplicacion.tareas-programadas.habilitadas", havingValue = "true", matchIfMissing = true)
public class ConfiguracionTareasProgramadas {
}
