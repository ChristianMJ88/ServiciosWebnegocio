package com.techprotech.agenda.compartido.whatsapp;

public final class NormalizadorTelefonoWhatsapp {

    private NormalizadorTelefonoWhatsapp() {
    }

    public static String normalizarComparable(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return "";
        }

        String sinPrefijo = telefono.trim().replace("whatsapp:", "");
        return sinPrefijo.replaceAll("[^0-9]", "");
    }

    public static boolean coincide(String telefonoA, String telefonoB) {
        String normalizadoA = normalizarComparable(telefonoA);
        String normalizadoB = normalizarComparable(telefonoB);

        if (normalizadoA.isBlank() || normalizadoB.isBlank()) {
            return false;
        }

        if (normalizadoA.equals(normalizadoB)) {
            return true;
        }

        return ultimosDigitos(normalizadoA, 10).equals(ultimosDigitos(normalizadoB, 10));
    }

    public static String aDireccionWhatsapp(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El telefono de destino es obligatorio");
        }

        String limpio = telefono.trim();
        if (limpio.startsWith("whatsapp:")) {
            return limpio;
        }

        String digitos = normalizarComparable(limpio);
        if (digitos.length() == 10) {
            digitos = "52" + digitos;
        }

        return "whatsapp:+" + digitos;
    }

    private static String ultimosDigitos(String valor, int longitud) {
        if (valor.length() <= longitud) {
            return valor;
        }
        return valor.substring(valor.length() - longitud);
    }
}
