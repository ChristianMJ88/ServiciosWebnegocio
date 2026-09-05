package com.techprotech.agenda.seguridad.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteVentanaFijaTest {

    @Test
    void rechazaSolicitudesQueExcedenLaVentana() {
        LimiteVentanaFija limite = new LimiteVentanaFija(
                Clock.fixed(Instant.parse("2026-09-05T12:00:10Z"), ZoneOffset.UTC)
        );

        assertTrue(limite.consumir("login:127.0.0.1", 2, 60).permitido());
        assertTrue(limite.consumir("login:127.0.0.1", 2, 60).permitido());
        assertFalse(limite.consumir("login:127.0.0.1", 2, 60).permitido());
    }

    @Test
    void mantieneLimitesIndependientesPorClave() {
        LimiteVentanaFija limite = new LimiteVentanaFija(
                Clock.fixed(Instant.parse("2026-09-05T12:00:10Z"), ZoneOffset.UTC)
        );

        assertTrue(limite.consumir("login:uno", 1, 60).permitido());
        assertFalse(limite.consumir("login:uno", 1, 60).permitido());
        assertTrue(limite.consumir("login:dos", 1, 60).permitido());
    }
}
