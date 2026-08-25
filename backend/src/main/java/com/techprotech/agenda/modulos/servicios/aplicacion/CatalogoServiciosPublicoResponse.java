package com.techprotech.agenda.modulos.servicios.aplicacion;

import java.util.List;

public record CatalogoServiciosPublicoResponse(
        Long empresaId,
        String slug,
        String nombreComercial,
        List<GrupoServicioPublicoResponse> grupos
) {
}
