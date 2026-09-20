package com.techprotech.agenda.compartido.media.aplicacion;

import com.techprotech.agenda.compartido.media.api.dto.ArchivoMediaAdminResponse;
import com.techprotech.agenda.compartido.media.api.dto.ConfiguracionMediaAdminResponse;
import com.techprotech.agenda.compartido.media.config.PropiedadesCloudinary;
import com.techprotech.agenda.compartido.media.dominio.AlmacenamientoMedia;
import com.techprotech.agenda.compartido.media.dominio.ArchivoAlmacenado;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import com.techprotech.agenda.compartido.media.infraestructura.EmpresaArchivoMediaEntidad;
import com.techprotech.agenda.compartido.media.infraestructura.EmpresaArchivoMediaRepositorio;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class ServicioMediaEmpresa {
    private static final Set<String> TIPOS_IMAGEN = Set.of("image/jpeg", "image/png", "image/webp", "image/avif");
    private final AlmacenamientoMedia almacenamiento;
    private final EmpresaArchivoMediaRepositorio repositorio;
    private final PropiedadesCloudinary propiedades;
    private final ResolutorCarpetaMedia resolutorCarpeta;

    public ServicioMediaEmpresa(AlmacenamientoMedia almacenamiento, EmpresaArchivoMediaRepositorio repositorio,
                                PropiedadesCloudinary propiedades, ResolutorCarpetaMedia resolutorCarpeta) {
        this.almacenamiento = almacenamiento;
        this.repositorio = repositorio;
        this.propiedades = propiedades;
        this.resolutorCarpeta = resolutorCarpeta;
    }

    public ConfiguracionMediaAdminResponse obtenerConfiguracion() {
        return new ConfiguracionMediaAdminResponse(
                almacenamiento.disponible(), propiedades.maxBytesImagen(), TIPOS_IMAGEN.stream().sorted().toList(),
                resolutorCarpeta.estructura()
        );
    }

    @Transactional
    public ArchivoMediaAdminResponse subirImagen(Long empresaId, Long usuarioId, CategoriaMedia categoria, MultipartFile archivo) {
        validarImagen(archivo);
        ArchivoAlmacenado almacenado = almacenamiento.subirImagen(empresaId, categoria, archivo);
        try {
            EmpresaArchivoMediaEntidad entidad = new EmpresaArchivoMediaEntidad();
            entidad.setEmpresaId(empresaId);
            entidad.setCategoria(categoria);
            entidad.setProveedor(almacenado.proveedor());
            entidad.setAssetId(almacenado.assetId());
            entidad.setPublicId(almacenado.publicId());
            entidad.setSecureUrl(almacenado.secureUrl());
            entidad.setFormato(almacenado.formato());
            entidad.setAncho(almacenado.ancho());
            entidad.setAlto(almacenado.alto());
            entidad.setBytes(almacenado.bytes());
            entidad.setNombreOriginal(limitar(archivo.getOriginalFilename(), 255));
            entidad.setContentType(archivo.getContentType());
            entidad.setCreadoPorUsuarioId(usuarioId);
            return mapear(repositorio.saveAndFlush(entidad));
        } catch (RuntimeException excepcion) {
            almacenamiento.eliminarImagen(almacenado.publicId());
            throw excepcion;
        }
    }

    public EmpresaArchivoMediaEntidad validarPropiedad(Long empresaId, Long mediaId, CategoriaMedia categoria) {
        EmpresaArchivoMediaEntidad media = repositorio.findByIdAndEmpresaId(mediaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no pertenece al tenant"));
        if (media.getCategoria() != categoria) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no pertenece a la categoría solicitada");
        }
        return media;
    }

    private void validarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen para cargar");
        }
        if (archivo.getSize() > propiedades.maxBytesImagen()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamaño máximo permitido");
        }
        if (!TIPOS_IMAGEN.contains(archivo.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El formato de imagen no está permitido");
        }
    }

    private ArchivoMediaAdminResponse mapear(EmpresaArchivoMediaEntidad entidad) {
        return new ArchivoMediaAdminResponse(entidad.getId(), entidad.getCategoria(), entidad.getSecureUrl(), entidad.getFormato(),
                entidad.getAncho(), entidad.getAlto(), entidad.getBytes(), entidad.getNombreOriginal());
    }

    private String limitar(String valor, int maximo) {
        if (valor == null) return null;
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
