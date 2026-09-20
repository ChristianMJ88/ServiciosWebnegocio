package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.GrupoServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.SubgrupoServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.GrupoServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.SubgrupoServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarServiciosDestacadosRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.ServicioDestacadoSeleccionAdminResponse;
import com.techprotech.agenda.modulos.sitio.api.dto.ServicioDestacadoSeleccionRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.ServiciosDestacadosAdminResponse;
import com.techprotech.agenda.modulos.sitio.api.dto.VistaServiciosDestacadosResponse;
import com.techprotech.agenda.modulos.sitio.dominio.TipoSeleccionServicioDestacado;
import com.techprotech.agenda.modulos.sitio.dominio.VistaServiciosDestacados;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioServicioDestacadoItemEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioServiciosDestacadosConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioServicioDestacadoItemRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioServiciosDestacadosConfigRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ServicioServiciosDestacadosSitio {
    private static final int LIMITE_PREDETERMINADO = 3;
    private static final int MAXIMO_ELEMENTOS_CONFIGURABLES = 24;

    private final EmpresaSitioServiciosDestacadosConfigRepositorio configRepositorio;
    private final EmpresaSitioServicioDestacadoItemRepositorio itemRepositorio;
    private final EmpresaSitioConfigRepositorio sitioRepositorio;
    private final GrupoServicioRepositorio grupoRepositorio;
    private final SubgrupoServicioRepositorio subgrupoRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio;
    private final ObjectMapper objectMapper;

    public ServicioServiciosDestacadosSitio(
            EmpresaSitioServiciosDestacadosConfigRepositorio configRepositorio,
            EmpresaSitioServicioDestacadoItemRepositorio itemRepositorio,
            EmpresaSitioConfigRepositorio sitioRepositorio,
            GrupoServicioRepositorio grupoRepositorio,
            SubgrupoServicioRepositorio subgrupoRepositorio,
            ServicioRepositorio servicioRepositorio,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio,
            ObjectMapper objectMapper
    ) {
        this.configRepositorio = configRepositorio;
        this.itemRepositorio = itemRepositorio;
        this.sitioRepositorio = sitioRepositorio;
        this.grupoRepositorio = grupoRepositorio;
        this.subgrupoRepositorio = subgrupoRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ServiciosDestacadosAdminResponse obtenerParaAdministracion(Long empresaId) {
        validarSitio(empresaId);
        EmpresaSitioServiciosDestacadosConfigEntidad config = obtenerConfig(empresaId);
        CatalogoContexto catalogo = cargarCatalogo(empresaId);
        List<EmpresaSitioServicioDestacadoItemEntidad> items = itemRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId);
        return mapearAdmin(config, items, catalogo);
    }

    @Transactional
    public ServiciosDestacadosAdminResponse actualizar(
            Long empresaId,
            Long usuarioActorId,
            ActualizarServiciosDestacadosRequest request
    ) {
        validarSitio(empresaId);
        VistaServiciosDestacados vista = parsearVista(request.vista());
        List<ServicioDestacadoSeleccionRequest> solicitados = validarYOrdenar(request.elementos());
        CatalogoContexto catalogo = cargarCatalogo(empresaId);
        solicitados.forEach(item -> validarReferencia(item, catalogo));

        EmpresaSitioServiciosDestacadosConfigEntidad config = obtenerConfig(empresaId);
        List<EmpresaSitioServicioDestacadoItemEntidad> anteriores = itemRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId);
        String antes = serializar(mapearAdmin(config, anteriores, catalogo));

        config.setVista(vista);
        config.setMostrarPrecio(request.mostrarPrecio());
        config.setMostrarDuracion(request.mostrarDuracion());
        config.setMostrarDescripcion(request.mostrarDescripcion());
        config.setLimite(request.limite());
        configRepositorio.save(config);

        itemRepositorio.deleteByEmpresaId(empresaId);
        itemRepositorio.flush();
        List<EmpresaSitioServicioDestacadoItemEntidad> guardables = new ArrayList<>();
        for (int orden = 0; orden < solicitados.size(); orden++) {
            ServicioDestacadoSeleccionRequest solicitado = solicitados.get(orden);
            EmpresaSitioServicioDestacadoItemEntidad item = new EmpresaSitioServicioDestacadoItemEntidad();
            item.setEmpresaId(empresaId);
            item.setTipo(parsearTipo(solicitado.tipo()));
            item.setReferenciaId(solicitado.referenciaId());
            item.setEtiqueta(normalizar(solicitado.etiqueta()));
            item.setOrden(orden);
            guardables.add(item);
        }
        List<EmpresaSitioServicioDestacadoItemEntidad> guardados = itemRepositorio.saveAll(guardables);
        ServiciosDestacadosAdminResponse respuesta = mapearAdmin(config, guardados, catalogo);
        registrarAuditoria(empresaId, usuarioActorId, antes, serializar(respuesta));
        return respuesta;
    }

    public ServiciosDestacadosPublicoResponse obtenerParaSitioPublico(Long empresaId) {
        EmpresaSitioServiciosDestacadosConfigEntidad config = obtenerConfig(empresaId);
        CatalogoContexto catalogo = cargarCatalogo(empresaId);
        List<EmpresaSitioServicioDestacadoItemEntidad> configurados = itemRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId);
        boolean seleccionAutomatica = configurados.isEmpty();
        List<EmpresaSitioServicioDestacadoItemEntidad> items = seleccionAutomatica
                ? seleccionAutomatica(empresaId, catalogo)
                : configurados;
        List<ServicioDestacadoPublicoItemResponse> elementos = items.stream()
                .map(item -> mapearPublico(item, catalogo))
                .filter(java.util.Objects::nonNull)
                .limit(config.getLimite())
                .toList();
        return new ServiciosDestacadosPublicoResponse(
                config.getVista().name(),
                config.isMostrarPrecio(),
                config.isMostrarDuracion(),
                config.isMostrarDescripcion(),
                config.getLimite(),
                seleccionAutomatica,
                elementos
        );
    }

    private List<EmpresaSitioServicioDestacadoItemEntidad> seleccionAutomatica(
            Long empresaId,
            CatalogoContexto catalogo
    ) {
        List<EmpresaSitioServicioDestacadoItemEntidad> resultado = new ArrayList<>();
        int orden = 0;
        for (GrupoServicioEntidad grupo : catalogo.gruposOrdenados()) {
            if (!grupo.isActivo() || catalogo.serviciosPorGrupo().getOrDefault(grupo.getId(), List.of()).isEmpty()) continue;
            EmpresaSitioServicioDestacadoItemEntidad item = new EmpresaSitioServicioDestacadoItemEntidad();
            item.setEmpresaId(empresaId);
            item.setTipo(TipoSeleccionServicioDestacado.GRUPO);
            item.setReferenciaId(grupo.getId());
            item.setOrden(orden++);
            resultado.add(item);
        }
        return resultado;
    }

    private ServicioDestacadoPublicoItemResponse mapearPublico(
            EmpresaSitioServicioDestacadoItemEntidad item,
            CatalogoContexto catalogo
    ) {
        return switch (item.getTipo()) {
            case GRUPO -> mapearGrupo(item, catalogo);
            case SUBGRUPO -> mapearSubgrupo(item, catalogo);
            case SERVICIO -> mapearServicio(item, catalogo);
        };
    }

    private ServicioDestacadoPublicoItemResponse mapearGrupo(
            EmpresaSitioServicioDestacadoItemEntidad item,
            CatalogoContexto catalogo
    ) {
        GrupoServicioEntidad grupo = catalogo.grupos().get(item.getReferenciaId());
        if (grupo == null || !grupo.isActivo()) return null;
        List<ServicioEntidad> servicios = catalogo.serviciosPorGrupo().getOrDefault(grupo.getId(), List.of());
        if (servicios.isEmpty()) return null;
        PrecioMoneda precio = precioMinimo(servicios);
        return new ServicioDestacadoPublicoItemResponse(
                item.getTipo().name(), grupo.getId(), grupo.getId(), null, null,
                grupo.getNombre(), grupo.getDescripcion(),
                primeraNoVacia(grupo.getImagenUrl(), primeraImagen(servicios)),
                precio.precio(), precio.moneda(), null, servicios.size(), item.getEtiqueta()
        );
    }

    private ServicioDestacadoPublicoItemResponse mapearSubgrupo(
            EmpresaSitioServicioDestacadoItemEntidad item,
            CatalogoContexto catalogo
    ) {
        SubgrupoServicioEntidad subgrupo = catalogo.subgrupos().get(item.getReferenciaId());
        GrupoServicioEntidad grupo = subgrupo != null ? catalogo.grupos().get(subgrupo.getGrupoId()) : null;
        if (subgrupo == null || !subgrupo.isActivo() || grupo == null || !grupo.isActivo()) return null;
        List<ServicioEntidad> servicios = catalogo.serviciosPorSubgrupo().getOrDefault(subgrupo.getId(), List.of());
        if (servicios.isEmpty()) return null;
        PrecioMoneda precio = precioMinimo(servicios);
        return new ServicioDestacadoPublicoItemResponse(
                item.getTipo().name(), subgrupo.getId(), grupo.getId(), subgrupo.getId(), null,
                subgrupo.getNombre(), subgrupo.getDescripcion(),
                primeraNoVacia(primeraImagen(servicios), grupo.getImagenUrl()),
                precio.precio(), precio.moneda(), null, servicios.size(), item.getEtiqueta()
        );
    }

    private ServicioDestacadoPublicoItemResponse mapearServicio(
            EmpresaSitioServicioDestacadoItemEntidad item,
            CatalogoContexto catalogo
    ) {
        ServicioEntidad servicio = catalogo.servicios().get(item.getReferenciaId());
        if (servicio == null || !servicio.isActivo() || !servicio.isVisiblePublico()) return null;
        return new ServicioDestacadoPublicoItemResponse(
                item.getTipo().name(), servicio.getId(), servicio.getGrupoId(), servicio.getSubgrupoId(), servicio.getId(),
                servicio.getNombre(), servicio.getDescripcion(), servicio.getImagenUrl(),
                servicio.getPrecio(), servicio.getMoneda(), servicio.getDuracionMinutos(), 1, item.getEtiqueta()
        );
    }

    private ServiciosDestacadosAdminResponse mapearAdmin(
            EmpresaSitioServiciosDestacadosConfigEntidad config,
            List<EmpresaSitioServicioDestacadoItemEntidad> items,
            CatalogoContexto catalogo
    ) {
        return new ServiciosDestacadosAdminResponse(
                config.getVista().name(),
                config.isMostrarPrecio(),
                config.isMostrarDuracion(),
                config.isMostrarDescripcion(),
                config.getLimite(),
                items.isEmpty(),
                List.of(VistaServiciosDestacados.values()).stream()
                        .map(vista -> new VistaServiciosDestacadosResponse(vista.name(), vista.nombre(), vista.descripcion()))
                        .toList(),
                items.stream().sorted(Comparator.comparingInt(EmpresaSitioServicioDestacadoItemEntidad::getOrden))
                        .map(item -> mapearSeleccionAdmin(item, catalogo))
                        .toList()
        );
    }

    private ServicioDestacadoSeleccionAdminResponse mapearSeleccionAdmin(
            EmpresaSitioServicioDestacadoItemEntidad item,
            CatalogoContexto catalogo
    ) {
        String nombre = null;
        String ruta = null;
        boolean disponible = false;
        if (item.getTipo() == TipoSeleccionServicioDestacado.GRUPO) {
            GrupoServicioEntidad grupo = catalogo.grupos().get(item.getReferenciaId());
            if (grupo != null) {
                nombre = grupo.getNombre();
                ruta = grupo.getNombre();
                disponible = grupo.isActivo();
            }
        } else if (item.getTipo() == TipoSeleccionServicioDestacado.SUBGRUPO) {
            SubgrupoServicioEntidad subgrupo = catalogo.subgrupos().get(item.getReferenciaId());
            GrupoServicioEntidad grupo = subgrupo != null ? catalogo.grupos().get(subgrupo.getGrupoId()) : null;
            if (subgrupo != null) {
                nombre = subgrupo.getNombre();
                ruta = grupo != null ? grupo.getNombre() + " / " + subgrupo.getNombre() : subgrupo.getNombre();
                disponible = subgrupo.isActivo() && grupo != null && grupo.isActivo();
            }
        } else {
            ServicioEntidad servicio = catalogo.servicios().get(item.getReferenciaId());
            GrupoServicioEntidad grupo = servicio != null ? catalogo.grupos().get(servicio.getGrupoId()) : null;
            SubgrupoServicioEntidad subgrupo = servicio != null ? catalogo.subgrupos().get(servicio.getSubgrupoId()) : null;
            if (servicio != null) {
                nombre = servicio.getNombre();
                ruta = construirRuta(grupo, subgrupo, servicio);
                disponible = servicio.isActivo() && servicio.isVisiblePublico();
            }
        }
        return new ServicioDestacadoSeleccionAdminResponse(
                item.getTipo().name(), item.getReferenciaId(), nombre, ruta,
                item.getEtiqueta(), item.getOrden(), disponible
        );
    }

    private String construirRuta(
            GrupoServicioEntidad grupo,
            SubgrupoServicioEntidad subgrupo,
            ServicioEntidad servicio
    ) {
        List<String> partes = new ArrayList<>();
        if (grupo != null) partes.add(grupo.getNombre());
        if (subgrupo != null) partes.add(subgrupo.getNombre());
        partes.add(servicio.getNombre());
        return String.join(" / ", partes);
    }

    private List<ServicioDestacadoSeleccionRequest> validarYOrdenar(List<ServicioDestacadoSeleccionRequest> items) {
        if (items.size() > MAXIMO_ELEMENTOS_CONFIGURABLES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La selección supera el máximo permitido");
        }
        Set<String> referencias = new HashSet<>();
        Set<Integer> posiciones = new HashSet<>();
        for (ServicioDestacadoSeleccionRequest item : items) {
            TipoSeleccionServicioDestacado tipo = parsearTipo(item.tipo());
            if (item.referenciaId() <= 0 || !referencias.add(tipo.name() + ":" + item.referenciaId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La selección contiene referencias inválidas o repetidas");
            }
            if (!posiciones.add(item.orden())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las posiciones no pueden repetirse");
            }
        }
        return items.stream().sorted(Comparator.comparingInt(ServicioDestacadoSeleccionRequest::orden)).toList();
    }

    private void validarReferencia(ServicioDestacadoSeleccionRequest item, CatalogoContexto catalogo) {
        TipoSeleccionServicioDestacado tipo = parsearTipo(item.tipo());
        boolean valida = switch (tipo) {
            case GRUPO -> {
                GrupoServicioEntidad grupo = catalogo.grupos().get(item.referenciaId());
                yield grupo != null && grupo.isActivo();
            }
            case SUBGRUPO -> {
                SubgrupoServicioEntidad subgrupo = catalogo.subgrupos().get(item.referenciaId());
                GrupoServicioEntidad grupo = subgrupo != null ? catalogo.grupos().get(subgrupo.getGrupoId()) : null;
                yield subgrupo != null && subgrupo.isActivo() && grupo != null && grupo.isActivo();
            }
            case SERVICIO -> {
                ServicioEntidad servicio = catalogo.servicios().get(item.referenciaId());
                yield servicio != null && servicio.isActivo() && servicio.isVisiblePublico();
            }
        };
        if (!valida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La selección contiene elementos que no pertenecen al catálogo público del tenant");
        }
    }

    private CatalogoContexto cargarCatalogo(Long empresaId) {
        List<GrupoServicioEntidad> gruposOrdenados = grupoRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId);
        List<SubgrupoServicioEntidad> subgruposOrdenados = subgrupoRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId);
        List<ServicioEntidad> serviciosOrdenados = servicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId);
        Map<Long, GrupoServicioEntidad> grupos = indexarGrupos(gruposOrdenados);
        Map<Long, SubgrupoServicioEntidad> subgrupos = indexarSubgrupos(subgruposOrdenados);
        Map<Long, ServicioEntidad> servicios = new HashMap<>();
        Map<Long, List<ServicioEntidad>> serviciosPorGrupo = new HashMap<>();
        Map<Long, List<ServicioEntidad>> serviciosPorSubgrupo = new HashMap<>();
        for (ServicioEntidad servicio : serviciosOrdenados) {
            servicios.put(servicio.getId(), servicio);
            if (!servicio.isActivo() || !servicio.isVisiblePublico()) continue;
            if (servicio.getGrupoId() != null) {
                serviciosPorGrupo.computeIfAbsent(servicio.getGrupoId(), ignored -> new ArrayList<>()).add(servicio);
            }
            if (servicio.getSubgrupoId() != null) {
                serviciosPorSubgrupo.computeIfAbsent(servicio.getSubgrupoId(), ignored -> new ArrayList<>()).add(servicio);
            }
        }
        return new CatalogoContexto(
                grupos, subgrupos, servicios, gruposOrdenados,
                serviciosPorGrupo, serviciosPorSubgrupo
        );
    }

    private Map<Long, GrupoServicioEntidad> indexarGrupos(List<GrupoServicioEntidad> grupos) {
        Map<Long, GrupoServicioEntidad> resultado = new HashMap<>();
        grupos.forEach(grupo -> resultado.put(grupo.getId(), grupo));
        return resultado;
    }

    private Map<Long, SubgrupoServicioEntidad> indexarSubgrupos(List<SubgrupoServicioEntidad> subgrupos) {
        Map<Long, SubgrupoServicioEntidad> resultado = new HashMap<>();
        subgrupos.forEach(subgrupo -> resultado.put(subgrupo.getId(), subgrupo));
        return resultado;
    }

    private EmpresaSitioServiciosDestacadosConfigEntidad obtenerConfig(Long empresaId) {
        return configRepositorio.findById(empresaId).orElseGet(() -> {
            EmpresaSitioServiciosDestacadosConfigEntidad config = new EmpresaSitioServiciosDestacadosConfigEntidad();
            config.setEmpresaId(empresaId);
            config.setVista(VistaServiciosDestacados.TARJETAS);
            config.setMostrarPrecio(true);
            config.setMostrarDuracion(true);
            config.setMostrarDescripcion(true);
            config.setLimite(LIMITE_PREDETERMINADO);
            return config;
        });
    }

    private void validarSitio(Long empresaId) {
        if (!sitioRepositorio.existsById(empresaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tenant no tiene configuración de sitio");
        }
    }

    private TipoSeleccionServicioDestacado parsearTipo(String valor) {
        try {
            return TipoSeleccionServicioDestacado.valueOf(valor.trim().toUpperCase());
        } catch (RuntimeException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de selección no reconocido");
        }
    }

    private VistaServiciosDestacados parsearVista(String valor) {
        try {
            return VistaServiciosDestacados.valueOf(valor.trim().toUpperCase());
        } catch (RuntimeException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vista de servicios destacados no reconocida");
        }
    }

    private PrecioMoneda precioMinimo(List<ServicioEntidad> servicios) {
        ServicioEntidad menor = servicios.stream()
                .filter(servicio -> servicio.getPrecio() != null)
                .min(Comparator.comparing(ServicioEntidad::getPrecio))
                .orElse(null);
        return menor == null ? new PrecioMoneda(null, null) : new PrecioMoneda(menor.getPrecio(), menor.getMoneda());
    }

    private String primeraImagen(List<ServicioEntidad> servicios) {
        return servicios.stream().map(ServicioEntidad::getImagenUrl)
                .filter(valor -> valor != null && !valor.isBlank()).findFirst().orElse(null);
    }

    private String primeraNoVacia(String primera, String segunda) {
        return primera != null && !primera.isBlank() ? primera : segunda;
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void registrarAuditoria(Long empresaId, Long usuarioActorId, String antes, String despues) {
        AuditoriaConfiguracionEmpresaEntidad auditoria = new AuditoriaConfiguracionEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setModulo("SITIO");
        auditoria.setAccion("SERVICIOS_DESTACADOS_ACTUALIZADOS");
        auditoria.setResumen("Se actualizó la selección y presentación de servicios destacados");
        auditoria.setDetalleAntesJson(antes);
        auditoria.setDetalleDespuesJson(despues);
        auditoriaRepositorio.save(auditoria);
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException excepcion) {
            throw new IllegalStateException("No fue posible serializar la configuración de servicios destacados", excepcion);
        }
    }

    private record PrecioMoneda(BigDecimal precio, String moneda) {}

    private record CatalogoContexto(
            Map<Long, GrupoServicioEntidad> grupos,
            Map<Long, SubgrupoServicioEntidad> subgrupos,
            Map<Long, ServicioEntidad> servicios,
            List<GrupoServicioEntidad> gruposOrdenados,
            Map<Long, List<ServicioEntidad>> serviciosPorGrupo,
            Map<Long, List<ServicioEntidad>> serviciosPorSubgrupo
    ) {}
}
