package com.techprotech.agenda.compartido.media.infraestructura;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.techprotech.agenda.compartido.media.aplicacion.ResolutorCarpetaMedia;
import com.techprotech.agenda.compartido.media.config.PropiedadesCloudinary;
import com.techprotech.agenda.compartido.media.dominio.AlmacenamientoMedia;
import com.techprotech.agenda.compartido.media.dominio.ArchivoAlmacenado;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class AlmacenamientoMediaCloudinary implements AlmacenamientoMedia {
    private static final Logger LOG = LoggerFactory.getLogger(AlmacenamientoMediaCloudinary.class);
    private final Cloudinary cloudinary;
    private final PropiedadesCloudinary propiedades;
    private final ResolutorCarpetaMedia resolutorCarpeta;

    public AlmacenamientoMediaCloudinary(PropiedadesCloudinary propiedades, ResolutorCarpetaMedia resolutorCarpeta) {
        this.propiedades = propiedades;
        this.resolutorCarpeta = resolutorCarpeta;
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", valorSeguro(propiedades.cloudName()),
                "api_key", valorSeguro(propiedades.apiKey()),
                "api_secret", valorSeguro(propiedades.apiSecret()),
                "secure", true
        ));
    }

    @Override
    public ArchivoAlmacenado subirImagen(Long empresaId, CategoriaMedia categoria, MultipartFile archivo) {
        if (!disponible()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El almacenamiento de imágenes aún no está configurado");
        }
        String carpeta = resolutorCarpeta.resolver(empresaId, categoria);
        try {
            Map<?, ?> resultado = cloudinary.uploader().upload(archivo.getBytes(), ObjectUtils.asMap(
                    "resource_type", "image",
                    "asset_folder", carpeta,
                    "public_id_prefix", carpeta,
                    "use_asset_folder_as_public_id_prefix", true,
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false,
                    "tags", List.of("fluora", "empresa-" + empresaId, categoria.carpeta())
            ));
            return new ArchivoAlmacenado(
                    "CLOUDINARY",
                    texto(resultado.get("asset_id")),
                    textoRequerido(resultado, "public_id"),
                    textoRequerido(resultado, "secure_url"),
                    texto(resultado.get("format")),
                    entero(resultado.get("width")),
                    entero(resultado.get("height")),
                    largo(resultado.get("bytes"))
            );
        } catch (IOException | RuntimeException excepcion) {
            LOG.warn("Cloudinary rechazó una carga para empresa {} y categoría {}: {}",
                    empresaId, categoria, excepcion.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary no pudo almacenar la imagen", excepcion);
        }
    }

    @Override
    public void eliminarImagen(String publicId) {
        if (!disponible() || publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (IOException | RuntimeException excepcion) {
            throw new IllegalStateException("No fue posible compensar la carga en Cloudinary", excepcion);
        }
    }

    @Override
    public boolean disponible() {
        return propiedades.configurado();
    }

    private static String valorSeguro(String valor) { return valor == null ? "" : valor; }
    private static String texto(Object valor) { return valor == null ? null : valor.toString(); }
    private static String textoRequerido(Map<?, ?> mapa, String llave) {
        String valor = texto(mapa.get(llave));
        if (valor == null || valor.isBlank()) throw new IllegalStateException("Cloudinary no devolvió " + llave);
        return valor;
    }
    private static Integer entero(Object valor) { return valor instanceof Number numero ? numero.intValue() : null; }
    private static long largo(Object valor) { return valor instanceof Number numero ? numero.longValue() : 0L; }
}
