package com.techprotech.agenda.compartido.media.api.dto;

import java.util.List;

public record ConfiguracionMediaAdminResponse(
        boolean cargaHabilitada,
        long maxBytesImagen,
        List<String> tiposImagenPermitidos,
        String estructuraCarpetas
) {}
