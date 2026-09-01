package com.techprotech.agenda.modulos.autenticacion.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.PropiedadesCorreo;
import com.techprotech.agenda.compartido.correo.ProtectorSecretosCorreo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioTokenRegistroSocialTest {
    private final ServicioTokenRegistroSocial tokens = new ServicioTokenRegistroSocial(
            new ProtectorSecretosCorreo(new PropiedadesCorreo(true, "SMTP", "no-reply@prueba.local",
                    "Prueba", null, null, null, null, null, null, null, "llave-prueba", 20, 60)),
            new ObjectMapper()
    );

    @Test
    void conservaProveedorYDestinoSinCompartirEstado() {
        String state = tokens.crearEstado("MICROSOFT", "ACCESO");
        assertEquals("ACCESO", tokens.validarEstado(state, "MICROSOFT"));
        assertThrows(IllegalArgumentException.class, () -> tokens.validarEstado(state, "GOOGLE"));
    }

    @Test
    void creaPerfilNeutralParaCualquierProveedor() {
        PerfilRegistroSocial original = new PerfilRegistroSocial(
                "MICROSOFT", "subject-123", "usuario@empresa.com", "Usuario Prueba");
        PerfilRegistroSocial resultado = tokens.validarToken(tokens.crearToken(original));
        assertEquals(original, resultado);
    }
}
