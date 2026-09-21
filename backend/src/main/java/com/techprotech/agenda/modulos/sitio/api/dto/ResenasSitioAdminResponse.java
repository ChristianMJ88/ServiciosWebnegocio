package com.techprotech.agenda.modulos.sitio.api.dto;

import java.util.List;

public record ResenasSitioAdminResponse(
        String googleMapsUrl,
        List<ResenaSitioAdminResponse> resenas,
        List<IndicadorConfianzaSitioAdminResponse> indicadores
) {
}
