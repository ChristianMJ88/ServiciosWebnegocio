package com.techprotech.agenda.modulos.sitio.aplicacion;

import java.util.List;

public record ServiciosDestacadosPublicoResponse(
        String vista,
        boolean mostrarPrecio,
        boolean mostrarDuracion,
        boolean mostrarDescripcion,
        int limite,
        boolean seleccionAutomatica,
        List<ServicioDestacadoPublicoItemResponse> elementos
) {
}
