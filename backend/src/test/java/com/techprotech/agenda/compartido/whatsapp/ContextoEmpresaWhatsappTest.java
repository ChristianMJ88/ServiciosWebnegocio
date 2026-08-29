package com.techprotech.agenda.compartido.whatsapp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContextoEmpresaWhatsappTest {

    @Test
    void restauraElContextoAunqueLaOperacionFalle() {
        ContextoEmpresaWhatsapp contexto = new ContextoEmpresaWhatsapp();

        assertThrows(IllegalArgumentException.class, () -> contexto.ejecutar(9L, () -> {
            assertEquals(9L, contexto.requerirEmpresaId());
            throw new IllegalArgumentException("fallo esperado");
        }));
        assertThrows(IllegalStateException.class, contexto::requerirEmpresaId);
    }
}
