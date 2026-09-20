package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarBloquesSitioRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.BloqueSitioAdminResponse;
import com.techprotech.agenda.modulos.sitio.api.dto.BloqueSitioRequest;
import com.techprotech.agenda.modulos.sitio.dominio.TipoBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.AlineacionBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.AnchoBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.ConfiguracionVisualBloquePredeterminada;
import com.techprotech.agenda.modulos.sitio.dominio.EspaciadoBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.PresentacionBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.SeparadorBloqueSitio;
import com.techprotech.agenda.modulos.sitio.dominio.SuperficieBloqueSitio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioBloqueEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioBloqueRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ServicioConstructorSitio {
    private final EmpresaSitioBloqueRepositorio bloquesRepositorio;
    private final EmpresaSitioConfigRepositorio configuracionRepositorio;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio;
    private final ObjectMapper objectMapper;

    public ServicioConstructorSitio(
            EmpresaSitioBloqueRepositorio bloquesRepositorio,
            EmpresaSitioConfigRepositorio configuracionRepositorio,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio,
            ObjectMapper objectMapper
    ) {
        this.bloquesRepositorio = bloquesRepositorio;
        this.configuracionRepositorio = configuracionRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<BloqueSitioAdminResponse> obtenerParaAdministracion(Long empresaId) {
        EmpresaSitioConfigEntidad configuracion = obtenerConfiguracion(empresaId);
        return mapearAdmin(resolverBloques(empresaId, configuracion));
    }

    public List<BloqueSitioPublicoResponse> obtenerParaSitioPublico(
            Long empresaId,
            EmpresaSitioConfigEntidad configuracion
    ) {
        return resolverBloques(empresaId, configuracion).stream()
                .filter(bloque -> bloque.getTipo().disponible())
                .map(bloque -> new BloqueSitioPublicoResponse(
                        bloque.getTipo().name(),
                        bloque.isHabilitado(),
                        bloque.getOrden(),
                        bloque.getSuperficie().name(),
                        bloque.getPresentacion().name(),
                        bloque.getAnchoContenido().name(),
                        bloque.getAlineacion().name(),
                        bloque.getSeparador().name(),
                        bloque.getEspaciado().name(),
                        bloque.getEtiqueta(),
                        bloque.getTitulo(),
                        bloque.getDescripcionVisual()
                ))
                .toList();
    }

    @Transactional
    public List<BloqueSitioAdminResponse> actualizar(
            Long empresaId,
            Long usuarioActorId,
            ActualizarBloquesSitioRequest request
    ) {
        EmpresaSitioConfigEntidad configuracion = obtenerConfiguracion(empresaId);
        List<EmpresaSitioBloqueEntidad> actuales = resolverBloques(empresaId, configuracion);
        List<BloqueSitioRequest> solicitados = validarYOrdenar(request.bloques());
        String antes = serializar(mapearAdmin(actuales));

        Map<TipoBloqueSitio, EmpresaSitioBloqueEntidad> porTipo = new EnumMap<>(TipoBloqueSitio.class);
        actuales.forEach(bloque -> porTipo.put(bloque.getTipo(), bloque));

        int temporal = 1000;
        for (EmpresaSitioBloqueEntidad bloque : actuales) {
            bloque.setOrden(temporal++);
        }
        bloquesRepositorio.saveAllAndFlush(actuales);

        List<EmpresaSitioBloqueEntidad> guardables = new ArrayList<>();
        for (int indice = 0; indice < solicitados.size(); indice++) {
            BloqueSitioRequest solicitado = solicitados.get(indice);
            TipoBloqueSitio tipo = parsearTipo(solicitado.tipo());
            EmpresaSitioBloqueEntidad bloque = porTipo.getOrDefault(tipo, nuevoBloque(empresaId, tipo));
            bloque.setOrden(indice);
            bloque.setHabilitado(tipo.disponible() && solicitado.habilitado());
            bloque.setSuperficie(parsearOpcion(solicitado.superficie(), SuperficieBloqueSitio.class, "superficie"));
            bloque.setPresentacion(parsearOpcion(solicitado.presentacion(), PresentacionBloqueSitio.class, "presentación"));
            bloque.setAnchoContenido(parsearOpcion(solicitado.anchoContenido(), AnchoBloqueSitio.class, "ancho"));
            bloque.setAlineacion(parsearOpcion(solicitado.alineacion(), AlineacionBloqueSitio.class, "alineación"));
            bloque.setSeparador(parsearOpcion(solicitado.separador(), SeparadorBloqueSitio.class, "separador"));
            bloque.setEspaciado(parsearOpcion(solicitado.espaciado(), EspaciadoBloqueSitio.class, "espaciado"));
            bloque.setEtiqueta(normalizarOpcional(solicitado.etiqueta()));
            bloque.setTitulo(normalizarOpcional(solicitado.titulo()));
            bloque.setDescripcionVisual(normalizarOpcional(solicitado.descripcionVisual()));
            guardables.add(bloque);
        }

        List<EmpresaSitioBloqueEntidad> guardados = bloquesRepositorio.saveAll(guardables).stream()
                .sorted(Comparator.comparingInt(EmpresaSitioBloqueEntidad::getOrden))
                .toList();
        sincronizarCompatibilidad(configuracion, guardados);
        configuracionRepositorio.save(configuracion);

        List<BloqueSitioAdminResponse> respuesta = mapearAdmin(guardados);
        registrarAuditoria(empresaId, usuarioActorId, antes, serializar(respuesta));
        return respuesta;
    }

    private EmpresaSitioConfigEntidad obtenerConfiguracion(Long empresaId) {
        return configuracionRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El tenant no tiene configuración de sitio"));
    }

    private List<EmpresaSitioBloqueEntidad> resolverBloques(Long empresaId, EmpresaSitioConfigEntidad configuracion) {
        List<EmpresaSitioBloqueEntidad> persistidos = bloquesRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId);
        if (!persistidos.isEmpty()) {
            return completarCatalogo(empresaId, persistidos);
        }
        return construirPredeterminados(empresaId, configuracion);
    }

    private List<EmpresaSitioBloqueEntidad> completarCatalogo(
            Long empresaId,
            List<EmpresaSitioBloqueEntidad> persistidos
    ) {
        Map<TipoBloqueSitio, EmpresaSitioBloqueEntidad> porTipo = new EnumMap<>(TipoBloqueSitio.class);
        persistidos.forEach(bloque -> porTipo.put(bloque.getTipo(), bloque));
        int orden = persistidos.stream().mapToInt(EmpresaSitioBloqueEntidad::getOrden).max().orElse(-1) + 1;
        List<EmpresaSitioBloqueEntidad> completos = new ArrayList<>(persistidos);
        for (TipoBloqueSitio tipo : TipoBloqueSitio.values()) {
            if (!porTipo.containsKey(tipo)) {
                EmpresaSitioBloqueEntidad bloque = nuevoBloque(empresaId, tipo);
                bloque.setOrden(orden++);
                completos.add(bloque);
            }
        }
        return completos.stream().sorted(Comparator.comparingInt(EmpresaSitioBloqueEntidad::getOrden)).toList();
    }

    private List<EmpresaSitioBloqueEntidad> construirPredeterminados(
            Long empresaId,
            EmpresaSitioConfigEntidad configuracion
    ) {
        boolean serviciosPrimero = "SERVICIOS_CONTACTO".equals(configuracion.getInicioOrdenSecciones());
        List<TipoBloqueSitio> orden = new ArrayList<>(List.of(
                TipoBloqueSitio.PORTADA,
                serviciosPrimero ? TipoBloqueSitio.SERVICIOS_DESTACADOS : TipoBloqueSitio.CONTACTO,
                serviciosPrimero ? TipoBloqueSitio.CONTACTO : TipoBloqueSitio.SERVICIOS_DESTACADOS,
                TipoBloqueSitio.BENEFICIOS,
                TipoBloqueSitio.EQUIPO,
                TipoBloqueSitio.GALERIA,
                TipoBloqueSitio.PROMOCIONES,
                TipoBloqueSitio.RESENAS,
                TipoBloqueSitio.PREGUNTAS_FRECUENTES,
                TipoBloqueSitio.MAPA
        ));
        List<EmpresaSitioBloqueEntidad> resultado = new ArrayList<>();
        for (int indice = 0; indice < orden.size(); indice++) {
            TipoBloqueSitio tipo = orden.get(indice);
            EmpresaSitioBloqueEntidad bloque = nuevoBloque(empresaId, tipo);
            bloque.setOrden(indice);
            bloque.setHabilitado(switch (tipo) {
                case PORTADA -> true;
                case SERVICIOS_DESTACADOS -> configuracion.isInicioMostrarServicios();
                case CONTACTO -> configuracion.isInicioMostrarContacto();
                default -> false;
            });
            resultado.add(bloque);
        }
        return resultado;
    }

    private List<BloqueSitioRequest> validarYOrdenar(List<BloqueSitioRequest> bloques) {
        if (bloques.size() != TipoBloqueSitio.values().length) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La configuración debe incluir todos los bloques disponibles");
        }
        Set<TipoBloqueSitio> tipos = new HashSet<>();
        Set<Integer> posiciones = new HashSet<>();
        for (BloqueSitioRequest bloque : bloques) {
            TipoBloqueSitio tipo = parsearTipo(bloque.tipo());
            if (!tipos.add(tipo) || !posiciones.add(bloque.orden())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los tipos y posiciones de los bloques no pueden repetirse");
            }
        }
        if (tipos.size() != TipoBloqueSitio.values().length) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La configuración contiene bloques desconocidos o incompletos");
        }
        return bloques.stream().sorted(Comparator.comparingInt(BloqueSitioRequest::orden)).toList();
    }

    private TipoBloqueSitio parsearTipo(String valor) {
        try {
            return TipoBloqueSitio.valueOf(valor.trim().toUpperCase());
        } catch (RuntimeException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de bloque no reconocido: " + valor);
        }
    }

    private EmpresaSitioBloqueEntidad nuevoBloque(Long empresaId, TipoBloqueSitio tipo) {
        EmpresaSitioBloqueEntidad bloque = new EmpresaSitioBloqueEntidad();
        bloque.setEmpresaId(empresaId);
        bloque.setTipo(tipo);
        ConfiguracionVisualBloquePredeterminada visual = ConfiguracionVisualBloquePredeterminada.para(tipo);
        bloque.setSuperficie(visual.superficie());
        bloque.setPresentacion(visual.presentacion());
        bloque.setAnchoContenido(visual.ancho());
        bloque.setAlineacion(visual.alineacion());
        bloque.setSeparador(visual.separador());
        bloque.setEspaciado(visual.espaciado());
        bloque.setEtiqueta(visual.etiqueta());
        bloque.setTitulo(visual.titulo());
        bloque.setDescripcionVisual(visual.descripcion());
        return bloque;
    }

    private List<BloqueSitioAdminResponse> mapearAdmin(List<EmpresaSitioBloqueEntidad> bloques) {
        return bloques.stream().map(bloque -> {
            TipoBloqueSitio tipo = bloque.getTipo();
            return new BloqueSitioAdminResponse(
                    tipo.name(), tipo.nombre(), tipo.descripcion(), tipo.icono(),
                    bloque.isHabilitado(), bloque.getOrden(), tipo.disponible(),
                    bloque.getSuperficie().name(), bloque.getPresentacion().name(),
                    bloque.getAnchoContenido().name(), bloque.getAlineacion().name(),
                    bloque.getSeparador().name(), bloque.getEspaciado().name(),
                    bloque.getEtiqueta(), bloque.getTitulo(), bloque.getDescripcionVisual()
            );
        }).toList();
    }

    private <T extends Enum<T>> T parsearOpcion(String valor, Class<T> tipo, String campo) {
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (RuntimeException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor de " + campo + " no reconocido");
        }
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void sincronizarCompatibilidad(
            EmpresaSitioConfigEntidad configuracion,
            List<EmpresaSitioBloqueEntidad> bloques
    ) {
        EmpresaSitioBloqueEntidad servicios = buscar(bloques, TipoBloqueSitio.SERVICIOS_DESTACADOS);
        EmpresaSitioBloqueEntidad contacto = buscar(bloques, TipoBloqueSitio.CONTACTO);
        configuracion.setInicioMostrarServicios(servicios.isHabilitado());
        configuracion.setInicioMostrarContacto(contacto.isHabilitado());
        configuracion.setInicioOrdenSecciones(
                servicios.getOrden() < contacto.getOrden() ? "SERVICIOS_CONTACTO" : "CONTACTO_SERVICIOS"
        );
    }

    private EmpresaSitioBloqueEntidad buscar(List<EmpresaSitioBloqueEntidad> bloques, TipoBloqueSitio tipo) {
        return bloques.stream().filter(bloque -> bloque.getTipo() == tipo).findFirst().orElseThrow();
    }

    private void registrarAuditoria(Long empresaId, Long usuarioActorId, String antes, String despues) {
        AuditoriaConfiguracionEmpresaEntidad auditoria = new AuditoriaConfiguracionEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setModulo("SITIO");
        auditoria.setAccion("BLOQUES_SITIO_ACTUALIZADOS");
        auditoria.setResumen("Se actualizó el orden y la visibilidad de las secciones del sitio");
        auditoria.setDetalleAntesJson(antes);
        auditoria.setDetalleDespuesJson(despues);
        auditoriaRepositorio.save(auditoria);
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException excepcion) {
            throw new IllegalStateException("No fue posible serializar la configuración de bloques", excepcion);
        }
    }
}
