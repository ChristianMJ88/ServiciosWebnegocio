package com.techprotech.agenda.compartido.media.dominio;

import org.springframework.web.multipart.MultipartFile;

public interface AlmacenamientoMedia {
    ArchivoAlmacenado subirImagen(Long empresaId, CategoriaMedia categoria, MultipartFile archivo);
    void eliminarImagen(String publicId);
    boolean disponible();
}
