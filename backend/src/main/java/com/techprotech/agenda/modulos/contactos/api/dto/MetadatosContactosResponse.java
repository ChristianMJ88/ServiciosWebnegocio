package com.techprotech.agenda.modulos.contactos.api.dto;

import java.util.List;

public record MetadatosContactosResponse(
        List<EstadoContactoResponse> estados,
        String estadoNuevo,
        String estadoEnProceso,
        String estadoAtendido
) {
    public record EstadoContactoResponse(String codigo, String etiqueta) {
    }
}
