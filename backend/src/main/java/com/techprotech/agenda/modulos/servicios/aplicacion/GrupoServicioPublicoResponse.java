package com.techprotech.agenda.modulos.servicios.aplicacion;

import java.util.List;

public record GrupoServicioPublicoResponse(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        String icono,
        int ordenPublico,
        List<SubgrupoServicioPublicoResponse> subgrupos
) {
}
