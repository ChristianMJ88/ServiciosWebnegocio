package com.techprotech.agenda.modulos.sitio.api.dto;

import java.util.List;

public record ServiciosDestacadosAdminResponse(
        String vista,
        boolean mostrarPrecio,
        boolean mostrarDuracion,
        boolean mostrarDescripcion,
        int limite,
        boolean seleccionAutomatica,
        List<VistaServiciosDestacadosResponse> vistasDisponibles,
        List<ServicioDestacadoSeleccionAdminResponse> elementos
) {
}
