package com.techprotech.agenda.compartido.tiempo;

import com.techprotech.agenda.compartido.config.ConfiguracionTareasProgramadas;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class TareasProgramadasConfiguracionTest {
    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracionTareasProgramadas.class);
    private static final String PROCESADOR = "org.springframework.context.annotation.internalScheduledAnnotationProcessor";

    @Test void tareasSiguenHabilitadasPorDefecto() {
        contexto.run(c -> assertThat(c.containsBean(PROCESADOR)).isTrue());
    }

    @Test void smokeDeshabilitaElProcesadorCompleto() {
        contexto.withPropertyValues("aplicacion.tareas-programadas.habilitadas=false")
                .run(c -> assertThat(c.containsBean(PROCESADOR)).isFalse());
    }
}
