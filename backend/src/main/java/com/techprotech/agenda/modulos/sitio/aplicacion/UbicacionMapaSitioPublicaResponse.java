package com.techprotech.agenda.modulos.sitio.aplicacion;

public record UbicacionMapaSitioPublicaResponse(
        Long sucursalId,
        String nombre,
        String direccion,
        String telefono,
        String mapaEmbedUrl,
        String indicacionesUrl
) {
}
