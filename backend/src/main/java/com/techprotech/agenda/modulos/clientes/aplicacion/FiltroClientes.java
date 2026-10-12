package com.techprotech.agenda.modulos.clientes.aplicacion;

import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public record FiltroClientes(String busqueda, String nombre, String telefono, LocalDate visitaDesde,
                            LocalDate visitaHasta, Citas citas, Orden orden) {
    public enum Citas {
        TODOS("Todos los clientes"), CON_AGENDADAS("Con citas próximas"), SIN_AGENDADAS("Sin citas próximas");
        public final String etiqueta;
        Citas(String etiqueta) { this.etiqueta = etiqueta; }
    }
    public enum Orden {
        NOMBRE_ASC("Nombre: A–Z"), NOMBRE_DESC("Nombre: Z–A"),
        TELEFONO_ASC("Teléfono: ascendente"), TELEFONO_DESC("Teléfono: descendente"),
        ULTIMA_VISITA_DESC("Visita más reciente"), ULTIMA_VISITA_ASC("Visita más antigua"),
        AGENDADAS_DESC("Más citas próximas");
        public final String etiqueta;
        Orden(String etiqueta) { this.etiqueta = etiqueta; }
    }
    public FiltroClientes {
        busqueda = normalizar(busqueda); nombre = normalizar(nombre); telefono = normalizar(telefono);
        citas = citas == null ? Citas.TODOS : citas;
        orden = orden == null ? Orden.NOMBRE_ASC : orden;
        if (visitaDesde != null && visitaHasta != null && visitaDesde.isAfter(visitaHasta)) {
            throw new ResponseStatusException(BAD_REQUEST, "La fecha inicial de visita debe ser anterior o igual a la final");
        }
    }
    private static String normalizar(String texto) { return texto == null ? "" : texto.trim().toLowerCase(java.util.Locale.ROOT); }
    public static <T extends Enum<T>> T opcion(Class<T> tipo, String valor) {
        if (valor == null || valor.isBlank()) return null;
        return Arrays.stream(tipo.getEnumConstants()).filter(opcion -> opcion.name().equals(valor)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Filtro u orden de clientes no válido"));
    }
}
