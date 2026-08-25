package com.techprotech.agenda.modulos.recepcion.aplicacion;

import java.util.List;

public final class CatalogosRecepcion {
    public static final String ESTADO_CITA_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_CITA_CONFIRMADA = "CONFIRMADA";
    public static final String ESTADO_ESPERA_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_ESPERA_NOTIFICADA = "NOTIFICADO";
    public static final List<String> ESTADOS_CITA_FINALIZABLES = List.of("PENDIENTE", "CONFIRMADA");
    public static final List<String> ESTADOS_CITA_CANCELABLES = List.of("PENDIENTE", "CONFIRMADA");
    public static final List<Opcion> ESTADOS_CITA = List.of(
            new Opcion("PENDIENTE", "Pendiente"),
            new Opcion("CONFIRMADA", "Confirmada"),
            new Opcion("FINALIZADA", "Finalizada"),
            new Opcion("CANCELADA", "Cancelada")
    );
    public static final List<Opcion> ESTADOS_ESPERA = List.of(
            new Opcion("PENDIENTE", "Pendiente"),
            new Opcion("NOTIFICADO", "Notificado")
    );

    private CatalogosRecepcion() {
    }

    public record Opcion(String codigo, String etiqueta) {
    }
}
