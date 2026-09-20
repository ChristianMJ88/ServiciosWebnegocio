package com.techprotech.agenda.compartido.media.dominio;

public record ArchivoAlmacenado(
        String proveedor,
        String assetId,
        String publicId,
        String secureUrl,
        String formato,
        Integer ancho,
        Integer alto,
        long bytes
) {}
