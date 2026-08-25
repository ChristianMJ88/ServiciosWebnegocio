package com.techprotech.agenda.modulos.servicios.aplicacion;

import java.util.List;

public record SubgrupoServicioPublicoResponse(
        Long id,
        String nombre,
        String descripcion,
        int ordenPublico,
        List<ServicioPublicoResponse> servicios
) {
}
