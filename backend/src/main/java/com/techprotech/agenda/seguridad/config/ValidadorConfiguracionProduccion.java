package com.techprotech.agenda.seguridad.config;

import com.techprotech.agenda.compartido.whatsapp.PropiedadesWhatsapp;
import com.techprotech.agenda.seguridad.jwt.PropiedadesJwt;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("prod")
public class ValidadorConfiguracionProduccion implements ApplicationRunner {

    private static final Set<String> SECRETOS_JWT_INSEGUROS = Set.of(
            "cambio-este-secreto-en-desarrollo",
            "agenda-dev-jwt-secreto-seguro-2026-32chars"
    );
    private static final Set<String> CONTRASENAS_DB_INSEGURAS = Set.of(
            "agenda_pass",
            "password",
            "root"
    );

    private final PropiedadesJwt jwt;
    private final PropiedadesWhatsapp whatsapp;
    private final Environment environment;

    public ValidadorConfiguracionProduccion(
            PropiedadesJwt jwt,
            PropiedadesWhatsapp whatsapp,
            Environment environment
    ) {
        this.jwt = jwt;
        this.whatsapp = whatsapp;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validarSecretoJwt();
        validarContrasenaBaseDatos();
        if (whatsapp.habilitado() && !whatsapp.validarFirmaWebhook()) {
            throw new IllegalStateException(
                    "En producción debe habilitarse la validación de firma de webhooks de Twilio"
            );
        }
    }

    private void validarSecretoJwt() {
        String secreto = jwt.secreto();
        if (secreto == null || secreto.length() < 32 || SECRETOS_JWT_INSEGUROS.contains(secreto)) {
            throw new IllegalStateException(
                    "JWT_SECRETO debe ser un secreto único de al menos 32 caracteres en producción"
            );
        }
    }

    private void validarContrasenaBaseDatos() {
        String contrasena = environment.getProperty("spring.datasource.password");
        if (contrasena == null || contrasena.isBlank() || CONTRASENAS_DB_INSEGURAS.contains(contrasena)) {
            throw new IllegalStateException(
                    "DB_PASSWORD debe configurarse con un valor seguro en producción"
            );
        }
    }
}
