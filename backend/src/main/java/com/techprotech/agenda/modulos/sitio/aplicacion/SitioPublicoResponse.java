package com.techprotech.agenda.modulos.sitio.aplicacion;

public record SitioPublicoResponse(
        Long empresaId,
        String slug,
        String nombreComercial,
        String dominioPrincipal,
        String logoUrl,
        String descripcionCorta,
        String colorPrimario,
        String colorSecundario,
        String fuenteTitulos,
        String fuenteCuerpo,
        String heroTitulo,
        String heroSubtitulo,
        String heroImagenUrl,
        String whatsapp,
        String telefono,
        String correo,
        String direccion,
        String instagramUrl,
        String facebookUrl,
        String tema,
        boolean publicado
) {
}
