package com.techprotech.agenda.modulos.pagos.infraestructura.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtectorSecretosPagoTest {
    @Test
    void cifraConIvAleatorioYRechazaTextoPlano() {
        ProtectorSecretosPago protector = new ProtectorSecretosPago(
                new PropiedadesSeguridadPagos("llave-de-prueba-con-suficiente-entropia"));

        String primero = protector.encriptar("access-token");
        String segundo = protector.encriptar("access-token");

        assertNotEquals(primero, segundo);
        assertEquals("access-token", protector.desencriptar(primero));
        assertThrows(IllegalArgumentException.class, () -> protector.desencriptar("access-token"));
    }
}
