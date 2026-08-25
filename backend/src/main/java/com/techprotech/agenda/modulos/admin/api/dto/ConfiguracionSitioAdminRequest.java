package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfiguracionSitioAdminRequest(
        @NotBlank @Size(max = 100) String slug,
        @NotBlank @Size(max = 150) String nombreComercial,
        @Size(max = 255) String dominioPrincipal,
        @Size(max = 255) String logoUrl,
        @Size(max = 255) String descripcionCorta,
        @Size(max = 20) String colorPrimario,
        @Size(max = 20) String colorSecundario,
        @Size(max = 40) String fuenteTitulos,
        @Size(max = 40) String fuenteCuerpo,
        @Size(max = 180) String heroTitulo,
        @Size(max = 500) String heroSubtitulo,
        @Size(max = 500) String heroImagenUrl,
        @Size(max = 30) String whatsapp,
        @Size(max = 30) String telefono,
        @Size(max = 150) String correo,
        @Size(max = 255) String direccion,
        @Size(max = 255) String instagramUrl,
        @Size(max = 255) String facebookUrl,
        @Size(max = 50) String tema,
        boolean publicado
) {
}
