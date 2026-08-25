package com.techprotech.agenda.modulos.contactos.aplicacion;

import java.util.Arrays;

public enum EstadoSolicitudContacto {
    NUEVO("Nuevo"),
    EN_PROCESO("En proceso"),
    ATENDIDO("Atendido"),
    CERRADO("Cerrado");

    private final String etiqueta;

    EstadoSolicitudContacto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static boolean esValido(String codigo) {
        return Arrays.stream(values()).anyMatch(estado -> estado.name().equals(codigo));
    }
}
