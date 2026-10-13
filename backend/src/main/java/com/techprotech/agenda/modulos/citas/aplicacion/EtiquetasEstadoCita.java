package com.techprotech.agenda.modulos.citas.aplicacion;
public final class EtiquetasEstadoCita {
    private EtiquetasEstadoCita() {}
    public static String de(String estado) {
        if(estado==null) return "";
        return switch(estado) {
            case "PENDIENTE_PAGO" -> "Apartada · esperando pago";
            case "LIBERADA_SIN_PAGO" -> "Liberada por falta de pago";
            case "PENDIENTE" -> "Pendiente de confirmación";
            case "CONFIRMADA" -> "Confirmada";
            case "FINALIZADA" -> "Finalizada";
            case "CANCELADA" -> "Cancelada";
            case "NO_ASISTIO" -> "No asistió";
            case "LIBERADA_SIN_CONFIRMACION" -> "Liberada sin confirmación";
            default -> estado;
        };
    }
}
