package com.techprotech.agenda.compartido.media.dominio;

public enum CategoriaMedia {
    GALERIA("galeria"),
    COLABORADORES("colaboradores"),
    SERVICIOS("servicios"),
    BRANDING("branding"),
    RESENAS("resenas");

    private final String carpeta;

    CategoriaMedia(String carpeta) {
        this.carpeta = carpeta;
    }

    public String carpeta() {
        return carpeta;
    }
}
