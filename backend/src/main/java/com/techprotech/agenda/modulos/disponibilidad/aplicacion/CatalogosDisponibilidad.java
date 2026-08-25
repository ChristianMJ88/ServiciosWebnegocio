package com.techprotech.agenda.modulos.disponibilidad.aplicacion;

import java.util.List;

public final class CatalogosDisponibilidad {

    public static final List<Opcion> TIPOS_SUJETO = List.of(
            new Opcion("SUCURSAL", "Sucursal"),
            new Opcion("PRESTADOR", "Prestador")
    );
    public static final List<Opcion> TIPOS_BLOQUEO = List.of(
            new Opcion("BLOQUEO", "Bloqueo"),
            new Opcion("DESCANSO", "Descanso"),
            new Opcion("VACACIONES", "Vacaciones"),
            new Opcion("HORARIO_ESPECIAL", "Horario especial")
    );
    public static final int INTERVALO_MINIMO_MINUTOS = 5;

    private CatalogosDisponibilidad() {
    }

    public static boolean contiene(List<Opcion> opciones, String valor) {
        return opciones.stream().anyMatch(opcion -> opcion.valor().equals(valor));
    }

    public record Opcion(String valor, String etiqueta) {
    }
}
