package com.techprotech.agenda.compartido.tiempo;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionReloj {
    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
