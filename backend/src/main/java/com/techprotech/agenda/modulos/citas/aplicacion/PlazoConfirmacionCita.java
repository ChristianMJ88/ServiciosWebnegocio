package com.techprotech.agenda.modulos.citas.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class PlazoConfirmacionCita {
    private PlazoConfirmacionCita() {}

    public static boolean vencido(LocalDateTime inicio, ZoneId zona, int horasAntes, Clock reloj) {
        return vencido(inicio, zona, horasAntes, null, reloj);
    }

    public static Instant limite(LocalDateTime inicio, ZoneId zona, int horasAntes, LocalDateTime creadaEnUtc) {
        Instant comienzo = inicio.atZone(zona).toInstant();
        Instant limiteNormal = comienzo.minus(Duration.ofHours(horasAntes));
        // Una reserva creada dentro del margen conserva tiempo para confirmar hasta su inicio.
        boolean creadaDentroDelMargen = creadaEnUtc != null
                && !creadaEnUtc.toInstant(ZoneOffset.UTC).isBefore(limiteNormal);
        return creadaDentroDelMargen ? comienzo : limiteNormal;
    }

    public static boolean vencido(LocalDateTime inicio, ZoneId zona, int horasAntes,
                                  LocalDateTime creadaEnUtc, Clock reloj) {
        return !reloj.instant().isBefore(limite(inicio, zona, horasAntes, creadaEnUtc));
    }
}
