package com.techprotech.agenda.modulos.citas.api.dto;

import java.util.List;

public record CitasMultiplesCreadasResponse(
        List<CitaCreadaResponse> citas,
        int total,
        String mensaje
) {
}
