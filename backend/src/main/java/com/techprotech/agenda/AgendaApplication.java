package com.techprotech.agenda;

import com.techprotech.agenda.compartido.correo.PropiedadesCorreo;
import com.techprotech.agenda.compartido.whatsapp.PropiedadesWhatsapp;
import com.techprotech.agenda.seguridad.jwt.PropiedadesJwt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@EnableConfigurationProperties({PropiedadesJwt.class, PropiedadesCorreo.class, PropiedadesWhatsapp.class})
public class AgendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgendaApplication.class, args);
    }
}
