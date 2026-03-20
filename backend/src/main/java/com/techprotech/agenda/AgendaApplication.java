package com.techprotech.agenda;

import com.techprotech.agenda.seguridad.jwt.PropiedadesJwt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PropiedadesJwt.class)
public class AgendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgendaApplication.class, args);
    }
}

