package com.techprotech.agenda.modulos.sitio.aplicacion;

import java.util.List;

public record ResenasSitioPublicoResponse(
        String googleMapsUrl,
        List<ResenaSitioPublicaResponse> resenas,
        List<IndicadorConfianzaSitioPublicoResponse> indicadores
) {
}
