package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import com.techprotech.agenda.compartido.media.infraestructura.EmpresaArchivoMediaEntidad;
import com.techprotech.agenda.compartido.media.aplicacion.ServicioMediaEmpresa;
import com.techprotech.agenda.compartido.media.aplicacion.TransformadorImagenCloudinary;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.api.dto.*;
import com.techprotech.agenda.modulos.sitio.dominio.VistaGaleriaSitio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioGaleriaConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioGaleriaImagenEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioGaleriaSitio {
    private static final String TITULO_PREDETERMINADO = "Nuestro trabajo";
    private final EmpresaSitioGaleriaConfigRepositorio configRepositorio;
    private final EmpresaSitioGaleriaImagenRepositorio imagenRepositorio;
    private final EmpresaSitioConfigRepositorio sitioRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio;
    private final ObjectMapper objectMapper;
    private final ServicioMediaEmpresa servicioMedia;
    private final TransformadorImagenCloudinary transformadorImagen;

    public ServicioGaleriaSitio(
            EmpresaSitioGaleriaConfigRepositorio configRepositorio,
            EmpresaSitioGaleriaImagenRepositorio imagenRepositorio,
            EmpresaSitioConfigRepositorio sitioRepositorio,
            ServicioRepositorio servicioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio,
            ObjectMapper objectMapper,
            ServicioMediaEmpresa servicioMedia,
            TransformadorImagenCloudinary transformadorImagen
    ) {
        this.configRepositorio = configRepositorio;
        this.imagenRepositorio = imagenRepositorio;
        this.sitioRepositorio = sitioRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.objectMapper = objectMapper;
        this.servicioMedia = servicioMedia;
        this.transformadorImagen = transformadorImagen;
    }

    @Transactional
    public GaleriaSitioAdminResponse obtenerParaAdministracion(Long empresaId) {
        validarSitio(empresaId);
        return mapearAdmin(empresaId);
    }

    @Transactional
    public GaleriaSitioAdminResponse actualizar(Long empresaId, Long usuarioActorId, ActualizarGaleriaSitioRequest request) {
        validarSitio(empresaId);
        List<GaleriaImagenSitioRequest> imagenes = request.imagenes().stream()
                .sorted(Comparator.comparingInt(GaleriaImagenSitioRequest::orden)).toList();
        validarReferencias(empresaId, imagenes);
        VistaGaleriaSitio vista = parsearVista(request.vista());
        String antes = serializar(mapearAdmin(empresaId));

        EmpresaSitioGaleriaConfigEntidad config = configRepositorio.findById(empresaId)
                .orElseGet(() -> nuevaConfiguracion(empresaId));
        config.setTitulo(request.titulo().trim());
        config.setDescripcion(normalizar(request.descripcion()));
        config.setVista(vista);
        config.setAmpliacionHabilitada(request.ampliacionHabilitada());
        configRepositorio.save(config);

        imagenRepositorio.deleteByEmpresaId(empresaId);
        imagenRepositorio.flush();
        for (int orden = 0; orden < imagenes.size(); orden++) {
            GaleriaImagenSitioRequest item = imagenes.get(orden);
            EmpresaSitioGaleriaImagenEntidad entidad = new EmpresaSitioGaleriaImagenEntidad();
            entidad.setEmpresaId(empresaId);
            entidad.setMediaId(item.mediaId());
            entidad.setImagenUrl(resolverUrlImagen(empresaId, item));
            entidad.setTextoAlternativo(item.textoAlternativo().trim());
            entidad.setTitulo(normalizar(item.titulo()));
            entidad.setDescripcion(normalizar(item.descripcion()));
            entidad.setServicioId(item.servicioId());
            entidad.setSucursalId(item.sucursalId());
            entidad.setActiva(item.activa());
            entidad.setOrden(orden);
            imagenRepositorio.save(entidad);
        }

        GaleriaSitioAdminResponse respuesta = mapearAdmin(empresaId);
        registrarAuditoria(empresaId, usuarioActorId, antes, serializar(respuesta));
        return respuesta;
    }

    @Transactional
    public GaleriaSitioPublicaResponse obtenerParaSitioPublico(Long empresaId) {
        EmpresaSitioGaleriaConfigEntidad config = configRepositorio.findById(empresaId)
                .orElseGet(() -> nuevaConfiguracion(empresaId));
        Map<Long, ServicioEntidad> servicios = serviciosPorId(empresaId);
        Map<Long, SucursalEntidad> sucursales = sucursalesPorId(empresaId);
        List<GaleriaImagenSitioPublicaResponse> imagenes = imagenRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .filter(EmpresaSitioGaleriaImagenEntidad::isActiva)
                .map(item -> new GaleriaImagenSitioPublicaResponse(
                        item.getId(), item.getImagenUrl(),
                        varianteGaleria(item.getImagenUrl(), 480),
                        varianteGaleria(item.getImagenUrl(), 960),
                        varianteGaleria(item.getImagenUrl(), 1440),
                        varianteGaleria(item.getImagenUrl(), 1920),
                        item.getTextoAlternativo(), item.getTitulo(), item.getDescripcion(),
                        item.getServicioId(), nombreServicio(servicios, item.getServicioId()),
                        item.getSucursalId(), nombreSucursal(sucursales, item.getSucursalId())
                )).toList();
        return new GaleriaSitioPublicaResponse(
                config.getTitulo(), config.getDescripcion(), config.getVista().name(), config.isAmpliacionHabilitada(), imagenes
        );
    }

    private GaleriaSitioAdminResponse mapearAdmin(Long empresaId) {
        EmpresaSitioGaleriaConfigEntidad config = configRepositorio.findById(empresaId)
                .orElseGet(() -> nuevaConfiguracion(empresaId));
        Map<Long, ServicioEntidad> servicios = serviciosPorId(empresaId);
        Map<Long, SucursalEntidad> sucursales = sucursalesPorId(empresaId);
        List<GaleriaImagenSitioAdminResponse> imagenes = imagenRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .map(item -> new GaleriaImagenSitioAdminResponse(
                        item.getId(), item.getMediaId(), item.getImagenUrl(), item.getTextoAlternativo(), item.getTitulo(), item.getDescripcion(),
                        item.getServicioId(), nombreServicio(servicios, item.getServicioId()),
                        item.getSucursalId(), nombreSucursal(sucursales, item.getSucursalId()), item.isActiva(), item.getOrden()
                )).toList();
        return new GaleriaSitioAdminResponse(
                config.getTitulo(), config.getDescripcion(), config.getVista().name(), config.isAmpliacionHabilitada(), imagenes
        );
    }

    private void validarReferencias(Long empresaId, List<GaleriaImagenSitioRequest> imagenes) {
        Set<Long> servicios = serviciosPorId(empresaId).keySet();
        Set<Long> sucursales = sucursalesPorId(empresaId).keySet();
        boolean invalida = imagenes.stream().anyMatch(item ->
                (item.servicioId() != null && !servicios.contains(item.servicioId()))
                        || (item.sucursalId() != null && !sucursales.contains(item.sucursalId())));
        if (invalida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La galería contiene servicios o sucursales de otro tenant");
        }
        imagenes.stream().filter(item -> item.mediaId() != null)
                .forEach(item -> servicioMedia.validarPropiedad(empresaId, item.mediaId(), CategoriaMedia.GALERIA));
    }

    private String resolverUrlImagen(Long empresaId, GaleriaImagenSitioRequest item) {
        if (item.mediaId() == null) return item.imagenUrl().trim();
        EmpresaArchivoMediaEntidad media = servicioMedia.validarPropiedad(empresaId, item.mediaId(), CategoriaMedia.GALERIA);
        if (!media.getSecureUrl().equals(item.imagenUrl().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL no corresponde a la imagen cargada");
        }
        return media.getSecureUrl();
    }

    private String varianteGaleria(String url, int ancho) {
        String variante = transformadorImagen.galeria(url, ancho);
        return Objects.equals(url, variante) ? null : variante;
    }

    private Map<Long, ServicioEntidad> serviciosPorId(Long empresaId) {
        return servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(ServicioEntidad::getId, Function.identity()));
    }

    private Map<Long, SucursalEntidad> sucursalesPorId(Long empresaId) {
        return sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, Function.identity()));
    }

    private EmpresaSitioGaleriaConfigEntidad nuevaConfiguracion(Long empresaId) {
        EmpresaSitioGaleriaConfigEntidad config = new EmpresaSitioGaleriaConfigEntidad();
        config.setEmpresaId(empresaId);
        config.setTitulo(TITULO_PREDETERMINADO);
        config.setVista(VistaGaleriaSitio.CUADRICULA);
        config.setAmpliacionHabilitada(true);
        return config;
    }

    private VistaGaleriaSitio parsearVista(String valor) {
        try {
            return VistaGaleriaSitio.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vista de galería no reconocida");
        }
    }

    private String nombreServicio(Map<Long, ServicioEntidad> servicios, Long id) {
        return id != null && servicios.containsKey(id) ? servicios.get(id).getNombre() : null;
    }

    private String nombreSucursal(Map<Long, SucursalEntidad> sucursales, Long id) {
        return id != null && sucursales.containsKey(id) ? sucursales.get(id).getNombre() : null;
    }

    private void validarSitio(Long empresaId) {
        if (!sitioRepositorio.existsById(empresaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tenant no tiene configuración de sitio");
        }
    }

    private String normalizar(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private void registrarAuditoria(Long empresaId, Long usuarioActorId, String antes, String despues) {
        AuditoriaConfiguracionEmpresaEntidad auditoria = new AuditoriaConfiguracionEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setModulo("SITIO");
        auditoria.setAccion("GALERIA_SITIO_ACTUALIZADA");
        auditoria.setResumen("Se actualizó la galería del sitio");
        auditoria.setDetalleAntesJson(antes);
        auditoria.setDetalleDespuesJson(despues);
        auditoriaRepositorio.save(auditoria);
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException excepcion) {
            throw new IllegalStateException("No fue posible serializar la galería", excepcion);
        }
    }
}
