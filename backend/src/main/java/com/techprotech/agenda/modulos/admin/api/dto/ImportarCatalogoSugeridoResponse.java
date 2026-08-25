package com.techprotech.agenda.modulos.admin.api.dto;

public record ImportarCatalogoSugeridoResponse(
        String sugerenciaId,
        String sugerenciaNombre,
        int gruposCreados,
        int subgruposCreados,
        int serviciosCreados,
        int serviciosReutilizados,
        String mensaje
) {
}
