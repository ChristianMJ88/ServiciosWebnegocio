package com.techprotech.agenda.compartido.media.api.dto;

import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;

public record ArchivoMediaAdminResponse(
        Long id,
        CategoriaMedia categoria,
        String secureUrl,
        String formato,
        Integer ancho,
        Integer alto,
        long bytes,
        String nombreOriginal
) {}
