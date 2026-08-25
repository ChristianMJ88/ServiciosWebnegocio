package com.techprotech.agenda.modulos.admin.api.dto;

public record CatalogoSugeridoAdminResponse(
        String id,
        String nombre,
        String descripcion,
        int grupos,
        int subgrupos,
        int servicios
) {
}
