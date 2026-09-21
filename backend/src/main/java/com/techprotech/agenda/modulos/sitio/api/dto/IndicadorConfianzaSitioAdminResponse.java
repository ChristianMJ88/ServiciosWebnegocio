package com.techprotech.agenda.modulos.sitio.api.dto;

public record IndicadorConfianzaSitioAdminResponse(
        Long id,
        String valor,
        String etiqueta,
        String icono,
        boolean activo,
        int orden
) {
}
