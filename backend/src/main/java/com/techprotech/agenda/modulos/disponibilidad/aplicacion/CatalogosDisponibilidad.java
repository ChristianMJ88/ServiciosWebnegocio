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

    public static com.techprotech.agenda.modulos.disponibilidad.api.dto.MetadatosDisponibilidadResponse metadatos() {
        var dias = java.util.Arrays.stream(java.time.DayOfWeek.values()).map(dia -> {
            String nombre = dia.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es-MX"));
            return new com.techprotech.agenda.modulos.disponibilidad.api.dto.MetadatosDisponibilidadResponse.OpcionNumeroResponse(
                    dia.getValue(), Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1));
        }).toList();
        return new com.techprotech.agenda.modulos.disponibilidad.api.dto.MetadatosDisponibilidadResponse(
                TIPOS_SUJETO.stream().map(opcion -> new com.techprotech.agenda.modulos.disponibilidad.api.dto.MetadatosDisponibilidadResponse.OpcionTextoResponse(opcion.valor(), opcion.etiqueta())).toList(),
                dias,
                TIPOS_BLOQUEO.stream().map(opcion -> new com.techprotech.agenda.modulos.disponibilidad.api.dto.MetadatosDisponibilidadResponse.OpcionTextoResponse(opcion.valor(), opcion.etiqueta())).toList(),
                INTERVALO_MINIMO_MINUTOS);
    }

    public static boolean contiene(List<Opcion> opciones, String valor) {
        return opciones.stream().anyMatch(opcion -> opcion.valor().equals(valor));
    }

    public record Opcion(String valor, String etiqueta) {
    }
}
