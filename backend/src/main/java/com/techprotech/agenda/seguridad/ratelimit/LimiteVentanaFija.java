package com.techprotech.agenda.seguridad.ratelimit;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class LimiteVentanaFija {

    private static final int MAXIMO_CLAVES = 50_000;

    private final Clock reloj;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final AtomicLong operaciones = new AtomicLong();

    LimiteVentanaFija(Clock reloj) {
        this.reloj = reloj;
    }

    Resultado consumir(String clave, int maximo, long duracionSegundos) {
        long ahora = reloj.instant().getEpochSecond();
        long inicio = ahora - Math.floorMod(ahora, duracionSegundos);
        long fin = inicio + duracionSegundos;
        Ventana ventana = ventanas.compute(clave, (ignorada, existente) -> {
            if (existente == null || existente.fin() <= ahora) {
                return new Ventana(fin, 1);
            }
            return new Ventana(existente.fin(), existente.solicitudes() + 1);
        });

        if ((operaciones.incrementAndGet() & 1023) == 0 || ventanas.size() > MAXIMO_CLAVES) {
            ventanas.entrySet().removeIf(entry -> entry.getValue().fin() <= ahora);
        }

        long reintentarEn = Math.max(1, inicio + duracionSegundos - ahora);
        return new Resultado(ventana.solicitudes() <= maximo, reintentarEn);
    }

    record Resultado(boolean permitido, long reintentarEnSegundos) {}

    private record Ventana(long fin, int solicitudes) {}
}
