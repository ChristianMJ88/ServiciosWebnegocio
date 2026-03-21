package com.techprotech.agenda.compartido.correo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectorSecretosCorreoTest {

    @Test
    void puedeCifrarYDescifrarUnSecreto() {
        ProtectorSecretosCorreo protector = new ProtectorSecretosCorreo(
                new PropiedadesCorreo(true, "SMTP", "fallback@agenda.local", "Agenda", null, null, null, null, null, null, null, "llave-prueba", 20, 60)
        );

        String cifrado = protector.encriptar("smtp-secret");
        String descifrado = protector.desencriptarSiNecesario(cifrado);

        assertTrue(cifrado.startsWith("enc:v1:"));
        assertEquals("smtp-secret", descifrado);
    }
}
