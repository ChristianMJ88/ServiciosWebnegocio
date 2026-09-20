package com.techprotech.agenda.compartido.media.aplicacion;

import com.techprotech.agenda.compartido.media.config.PropiedadesCloudinary;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;

import org.springframework.stereotype.Component;

@Component
public class ResolutorCarpetaMedia {
    private final String raiz;

    public ResolutorCarpetaMedia(PropiedadesCloudinary propiedades) {
        String configurada = propiedades.carpetaRaiz() == null ? "" : propiedades.carpetaRaiz().trim();
        if (!configurada.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalStateException("CLOUDINARY_CARPETA_RAIZ solo admite letras, números, guion y guion bajo");
        }
        this.raiz = configurada;
    }

    public String resolver(Long empresaId, CategoriaMedia categoria) {
        if (empresaId == null || empresaId <= 0) {
            throw new IllegalArgumentException("La empresa es obligatoria para almacenar archivos");
        }
        return "%s/multitenant/%d/%s".formatted(raiz, empresaId, categoria.carpeta());
    }

    public String estructura() {
        return "%s/multitenant/{empresaId}/{categoria}".formatted(raiz);
    }
}
