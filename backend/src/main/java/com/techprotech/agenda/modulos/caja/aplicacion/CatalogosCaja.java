package com.techprotech.agenda.modulos.caja.aplicacion;

import java.util.List;

public final class CatalogosCaja {
    public static final String ESTADO_SESION_ABIERTA = "ABIERTA";
    public static final String ESTADO_SESION_CERRADA = "CERRADA";
    public static final String METODO_PAGO_EFECTIVO = "EFECTIVO";
    public static final List<Opcion> METODOS_PAGO = List.of(
            new Opcion("EFECTIVO", "Efectivo"),
            new Opcion("TARJETA", "Tarjeta"),
            new Opcion("TRANSFERENCIA", "Transferencia")
    );
    public static final List<Opcion> TIPOS_MOVIMIENTO_MANUAL = List.of(
            new Opcion("GASTO_MENOR", "Gasto menor"),
            new Opcion("RETIRO", "Retiro de efectivo"),
            new Opcion("INGRESO_EXTRA", "Ingreso extra"),
            new Opcion("AJUSTE_POSITIVO", "Ajuste positivo"),
            new Opcion("AJUSTE_NEGATIVO", "Ajuste negativo")
    );

    private CatalogosCaja() {
    }

    public static boolean contiene(List<Opcion> opciones, String codigo) {
        return opciones.stream().anyMatch(opcion -> opcion.codigo().equals(codigo));
    }

    public record Opcion(String codigo, String etiqueta) {
    }
}
