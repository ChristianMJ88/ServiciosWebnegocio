package com.techprotech.agenda.seguridad.config;

import com.techprotech.agenda.compartido.whatsapp.PropiedadesWhatsapp;
import com.techprotech.agenda.seguridad.jwt.PropiedadesJwt;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorConfiguracionProduccionTest {

    @Test
    void rechazaValoresDeDesarrolloEnProduccion() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("spring.datasource.password", "agenda_pass");
        ValidadorConfiguracionProduccion validador = new ValidadorConfiguracionProduccion(
                new PropiedadesJwt("cambio-este-secreto-en-desarrollo", "agenda", 15, 7),
                propiedadesWhatsapp(false, true),
                entorno
        );

        assertThrows(IllegalStateException.class, () -> validador.run(null));
    }

    @Test
    void aceptaSecretosFuertesYFirmaWebhook() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("spring.datasource.password", "una-clave-de-base-de-datos-no-predeterminada");
        ValidadorConfiguracionProduccion validador = new ValidadorConfiguracionProduccion(
                new PropiedadesJwt("secreto-unico-produccion-con-mas-de-32-caracteres", "agenda", 15, 7),
                propiedadesWhatsapp(true, true),
                entorno
        );

        assertDoesNotThrow(() -> validador.run(null));
    }

    @Test
    void rechazaWebhookSinFirmaCuandoWhatsappEstaHabilitado() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("spring.datasource.password", "una-clave-de-base-de-datos-no-predeterminada");
        ValidadorConfiguracionProduccion validador = new ValidadorConfiguracionProduccion(
                new PropiedadesJwt("secreto-unico-produccion-con-mas-de-32-caracteres", "agenda", 15, 7),
                propiedadesWhatsapp(true, false),
                entorno
        );

        assertThrows(IllegalStateException.class, () -> validador.run(null));
    }

    private PropiedadesWhatsapp propiedadesWhatsapp(boolean habilitado, boolean validarFirma) {
        return new PropiedadesWhatsapp(
                habilitado, validarFirma,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                20, 60, 24, 60, 2
        );
    }
}
