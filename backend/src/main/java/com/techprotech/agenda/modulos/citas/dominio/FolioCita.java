package com.techprotech.agenda.modulos.citas.dominio;

import java.security.SecureRandom;

/** Identificador público aleatorio; no sustituye autenticación ni autoriza consultar una reserva. */
public final class FolioCita {
    private static final char[] ALFABETO = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private FolioCita() {}
    public static String nuevo() {
        var resultado = new StringBuilder("FL-");
        for (int i = 0; i < 8; i++) resultado.append(ALFABETO[ALEATORIO.nextInt(ALFABETO.length)]);
        return resultado.toString();
    }
    public static String miembro(String reserva, int indice) {
        if (indice < 1) throw new IllegalArgumentException("El índice de una cita debe ser positivo");
        return reserva + "-" + indice;
    }
}
