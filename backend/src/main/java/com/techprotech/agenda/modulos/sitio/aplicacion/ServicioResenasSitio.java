package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.compartido.media.aplicacion.ServicioMediaEmpresa;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import com.techprotech.agenda.compartido.media.infraestructura.EmpresaArchivoMediaEntidad;
import com.techprotech.agenda.modulos.sitio.api.dto.*;
import com.techprotech.agenda.modulos.sitio.dominio.IconoIndicadorConfianza;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioIndicadorConfianzaEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioResenaConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioResenaEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioIndicadorConfianzaRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioResenaConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioResenaRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioResenasSitio {
    private final EmpresaSitioResenaConfigRepositorio configRepositorio;
    private final EmpresaSitioResenaRepositorio resenaRepositorio;
    private final EmpresaSitioIndicadorConfianzaRepositorio indicadorRepositorio;
    private final EmpresaSitioConfigRepositorio sitioRepositorio;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio;
    private final ObjectMapper objectMapper;
    private final ServicioMediaEmpresa servicioMedia;

    public ServicioResenasSitio(
            EmpresaSitioResenaConfigRepositorio configRepositorio,
            EmpresaSitioResenaRepositorio resenaRepositorio,
            EmpresaSitioIndicadorConfianzaRepositorio indicadorRepositorio,
            EmpresaSitioConfigRepositorio sitioRepositorio,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio,
            ObjectMapper objectMapper,
            ServicioMediaEmpresa servicioMedia
    ) {
        this.configRepositorio = configRepositorio;
        this.resenaRepositorio = resenaRepositorio;
        this.indicadorRepositorio = indicadorRepositorio;
        this.sitioRepositorio = sitioRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.objectMapper = objectMapper;
        this.servicioMedia = servicioMedia;
    }

    @Transactional
    public ResenasSitioAdminResponse obtenerParaAdministracion(Long empresaId) {
        validarSitio(empresaId);
        return mapearAdmin(empresaId);
    }

    @Transactional
    public ResenasSitioAdminResponse actualizar(Long empresaId, Long usuarioActorId, ActualizarResenasSitioRequest request) {
        validarSitio(empresaId);
        String antes = serializar(mapearAdmin(empresaId));

        EmpresaSitioResenaConfigEntidad config = configRepositorio.findById(empresaId)
                .orElseGet(() -> nuevaConfiguracion(empresaId));
        config.setGoogleMapsUrl(normalizarUrl(request.googleMapsUrl()));
        configRepositorio.save(config);

        List<ResenaSitioRequest> resenas = request.resenas().stream()
                .sorted(Comparator.comparingInt(ResenaSitioRequest::orden)).toList();
        resenas.forEach(item -> validarFoto(empresaId, item));
        resenaRepositorio.deleteByEmpresaId(empresaId);
        resenaRepositorio.flush();
        for (int orden = 0; orden < resenas.size(); orden++) {
            ResenaSitioRequest item = resenas.get(orden);
            EmpresaSitioResenaEntidad entidad = new EmpresaSitioResenaEntidad();
            entidad.setEmpresaId(empresaId);
            entidad.setNombre(item.nombre().trim());
            entidad.setCalificacion(item.calificacion());
            entidad.setComentario(item.comentario().trim());
            entidad.setMediaId(item.mediaId());
            entidad.setFotoUrl(resolverFoto(empresaId, item));
            entidad.setDestacada(item.destacada());
            entidad.setActiva(item.activa());
            entidad.setOrden(orden);
            resenaRepositorio.save(entidad);
        }

        List<IndicadorConfianzaSitioRequest> indicadores = request.indicadores().stream()
                .sorted(Comparator.comparingInt(IndicadorConfianzaSitioRequest::orden)).toList();
        indicadorRepositorio.deleteByEmpresaId(empresaId);
        indicadorRepositorio.flush();
        for (int orden = 0; orden < indicadores.size(); orden++) {
            IndicadorConfianzaSitioRequest item = indicadores.get(orden);
            EmpresaSitioIndicadorConfianzaEntidad entidad = new EmpresaSitioIndicadorConfianzaEntidad();
            entidad.setEmpresaId(empresaId);
            entidad.setValor(item.valor().trim());
            entidad.setEtiqueta(item.etiqueta().trim());
            entidad.setIcono(parsearIcono(item.icono()));
            entidad.setActivo(item.activo());
            entidad.setOrden(orden);
            indicadorRepositorio.save(entidad);
        }

        ResenasSitioAdminResponse respuesta = mapearAdmin(empresaId);
        registrarAuditoria(empresaId, usuarioActorId, antes, serializar(respuesta));
        return respuesta;
    }

    public ResenasSitioPublicoResponse obtenerParaSitioPublico(Long empresaId) {
        String googleMapsUrl = configRepositorio.findById(empresaId)
                .map(EmpresaSitioResenaConfigEntidad::getGoogleMapsUrl).orElse(null);
        List<ResenaSitioPublicaResponse> resenas = resenaRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .filter(EmpresaSitioResenaEntidad::isActiva)
                .map(item -> new ResenaSitioPublicaResponse(
                        item.getId(), item.getNombre(), item.getCalificacion(), item.getComentario(), item.getFotoUrl(), item.isDestacada()
                )).toList();
        List<IndicadorConfianzaSitioPublicoResponse> indicadores = indicadorRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .filter(EmpresaSitioIndicadorConfianzaEntidad::isActivo)
                .map(item -> new IndicadorConfianzaSitioPublicoResponse(
                        item.getId(), item.getValor(), item.getEtiqueta(), item.getIcono().name()
                )).toList();
        return new ResenasSitioPublicoResponse(googleMapsUrl, resenas, indicadores);
    }

    private ResenasSitioAdminResponse mapearAdmin(Long empresaId) {
        String googleMapsUrl = configRepositorio.findById(empresaId)
                .map(EmpresaSitioResenaConfigEntidad::getGoogleMapsUrl).orElse(null);
        List<ResenaSitioAdminResponse> resenas = resenaRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .map(item -> new ResenaSitioAdminResponse(
                        item.getId(), item.getNombre(), item.getCalificacion(), item.getComentario(), item.getMediaId(), item.getFotoUrl(),
                        item.isDestacada(), item.isActiva(), item.getOrden()
                )).toList();
        List<IndicadorConfianzaSitioAdminResponse> indicadores = indicadorRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .map(item -> new IndicadorConfianzaSitioAdminResponse(
                        item.getId(), item.getValor(), item.getEtiqueta(), item.getIcono().name(),
                        item.isActivo(), item.getOrden()
                )).toList();
        return new ResenasSitioAdminResponse(googleMapsUrl, resenas, indicadores);
    }

    private EmpresaSitioResenaConfigEntidad nuevaConfiguracion(Long empresaId) {
        EmpresaSitioResenaConfigEntidad entidad = new EmpresaSitioResenaConfigEntidad();
        entidad.setEmpresaId(empresaId);
        return entidad;
    }

    private void validarFoto(Long empresaId, ResenaSitioRequest item) {
        if (item.mediaId() == null) {
            if (item.fotoUrl() != null && !item.fotoUrl().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carga la foto del cliente desde el administrador");
            return;
        }
        EmpresaArchivoMediaEntidad media = servicioMedia.validarPropiedad(empresaId, item.mediaId(), CategoriaMedia.RESENAS);
        if (item.fotoUrl() == null || !media.getSecureUrl().equals(item.fotoUrl().trim()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto no corresponde al tenant");
    }

    private String resolverFoto(Long empresaId, ResenaSitioRequest item) {
        if (item.mediaId() == null) return null;
        return servicioMedia.validarPropiedad(empresaId, item.mediaId(), CategoriaMedia.RESENAS).getSecureUrl();
    }

    private IconoIndicadorConfianza parsearIcono(String valor) {
        try {
            return IconoIndicadorConfianza.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Icono de confianza no reconocido");
        }
    }

    private String normalizarUrl(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String limpia = valor.trim();
        try {
            URI uri = URI.create(limpia);
            if (uri.getHost() == null || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
            return limpia;
        } catch (IllegalArgumentException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace de Google Maps no es válido");
        }
    }

    private void validarSitio(Long empresaId) {
        if (!sitioRepositorio.existsById(empresaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tenant no tiene configuración de sitio");
        }
    }

    private void registrarAuditoria(Long empresaId, Long usuarioActorId, String antes, String despues) {
        AuditoriaConfiguracionEmpresaEntidad auditoria = new AuditoriaConfiguracionEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setModulo("SITIO");
        auditoria.setAccion("RESENAS_SITIO_ACTUALIZADAS");
        auditoria.setResumen("Se actualizaron las reseñas y señales de confianza del sitio");
        auditoria.setDetalleAntesJson(antes);
        auditoria.setDetalleDespuesJson(despues);
        auditoriaRepositorio.save(auditoria);
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException excepcion) {
            throw new IllegalStateException("No fue posible serializar las reseñas", excepcion);
        }
    }
}
