package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarPromocionesSitioRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.PromocionSitioAdminResponse;
import com.techprotech.agenda.modulos.sitio.api.dto.PromocionSitioRequest;
import com.techprotech.agenda.modulos.sitio.dominio.TipoDescuentoPromocion;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioPromocionesSitio {
    private final EmpresaSitioPromocionRepositorio promocionRepositorio;
    private final EmpresaSitioPromocionServicioRepositorio promocionServicioRepositorio;
    private final EmpresaSitioPromocionSucursalRepositorio promocionSucursalRepositorio;
    private final EmpresaSitioConfigRepositorio sitioRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio;
    private final ObjectMapper objectMapper;

    public ServicioPromocionesSitio(
            EmpresaSitioPromocionRepositorio promocionRepositorio,
            EmpresaSitioPromocionServicioRepositorio promocionServicioRepositorio,
            EmpresaSitioPromocionSucursalRepositorio promocionSucursalRepositorio,
            EmpresaSitioConfigRepositorio sitioRepositorio,
            ServicioRepositorio servicioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio,
            ObjectMapper objectMapper
    ) {
        this.promocionRepositorio = promocionRepositorio;
        this.promocionServicioRepositorio = promocionServicioRepositorio;
        this.promocionSucursalRepositorio = promocionSucursalRepositorio;
        this.sitioRepositorio = sitioRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<PromocionSitioAdminResponse> obtenerParaAdministracion(Long empresaId) {
        validarSitio(empresaId);
        return mapearAdmin(empresaId);
    }

    @Transactional
    public List<PromocionSitioAdminResponse> actualizar(
            Long empresaId,
            Long usuarioActorId,
            ActualizarPromocionesSitioRequest request
    ) {
        validarSitio(empresaId);
        List<PromocionSitioRequest> promociones = request.promociones().stream()
                .sorted(Comparator.comparingInt(PromocionSitioRequest::orden))
                .toList();
        validarPromociones(empresaId, promociones);
        String antes = serializar(mapearAdmin(empresaId));

        promocionServicioRepositorio.deleteByEmpresaId(empresaId);
        promocionSucursalRepositorio.deleteByEmpresaId(empresaId);
        promocionRepositorio.deleteByEmpresaId(empresaId);
        promocionRepositorio.flush();

        for (int orden = 0; orden < promociones.size(); orden++) {
            PromocionSitioRequest requestItem = promociones.get(orden);
            EmpresaSitioPromocionEntidad promocion = new EmpresaSitioPromocionEntidad();
            promocion.setEmpresaId(empresaId);
            promocion.setTitulo(requestItem.titulo().trim());
            promocion.setDescripcion(normalizar(requestItem.descripcion()));
            promocion.setImagenUrl(normalizar(requestItem.imagenUrl()));
            promocion.setFechaInicio(requestItem.fechaInicio());
            promocion.setFechaFin(requestItem.fechaFin());
            promocion.setTipoDescuento(parsearTipo(requestItem.tipoDescuento()));
            promocion.setValorDescuento(requestItem.valorDescuento());
            promocion.setMoneda(requestItem.moneda().trim().toUpperCase(Locale.ROOT));
            promocion.setTextoBoton(valorConFallback(requestItem.textoBoton(), "Reservar promoción"));
            promocion.setActiva(requestItem.activa());
            promocion.setOrden(orden);
            promocion = promocionRepositorio.saveAndFlush(promocion);
            guardarRelaciones(empresaId, promocion.getId(), requestItem);
        }

        List<PromocionSitioAdminResponse> respuesta = mapearAdmin(empresaId);
        registrarAuditoria(empresaId, usuarioActorId, antes, serializar(respuesta));
        return respuesta;
    }

    @Transactional
    public List<PromocionSitioPublicaResponse> obtenerParaSitioPublico(Long empresaId) {
        LocalDate hoy = LocalDate.now();
        Map<Long, ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(servicio -> servicio.isActivo() && servicio.isVisiblePublico())
                .collect(Collectors.toMap(ServicioEntidad::getId, Function.identity()));
        Map<Long, SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .filter(SucursalEntidad::isActiva)
                .collect(Collectors.toMap(SucursalEntidad::getId, Function.identity()));
        Map<Long, List<Long>> serviciosPorPromocion = agruparServicios(empresaId);
        Map<Long, List<Long>> sucursalesPorPromocion = agruparSucursales(empresaId);

        return promocionRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .filter(promocion -> promocion.isActiva() && vigente(promocion, hoy))
                .map(promocion -> {
                    List<Long> servicioIds = serviciosPorPromocion.getOrDefault(promocion.getId(), List.of()).stream()
                            .filter(servicios::containsKey).toList();
                    List<String> nombresServicios = servicioIds.stream().map(id -> servicios.get(id).getNombre()).toList();
                    List<String> nombresSucursales = sucursalesPorPromocion.getOrDefault(promocion.getId(), List.of()).stream()
                            .filter(sucursales::containsKey).map(id -> sucursales.get(id).getNombre()).toList();
                    return new PromocionSitioPublicaResponse(
                            promocion.getId(), promocion.getTitulo(), promocion.getDescripcion(), promocion.getImagenUrl(),
                            promocion.getFechaInicio(), promocion.getFechaFin(), promocion.getTipoDescuento().name(),
                            promocion.getValorDescuento(), promocion.getMoneda(), promocion.getTextoBoton(), servicioIds, nombresServicios, nombresSucursales
                    );
                }).toList();
    }

    private void validarPromociones(Long empresaId, List<PromocionSitioRequest> promociones) {
        Set<Long> serviciosValidos = servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .map(ServicioEntidad::getId).collect(Collectors.toSet());
        Set<Long> sucursalesValidas = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .map(SucursalEntidad::getId).collect(Collectors.toSet());
        for (PromocionSitioRequest promocion : promociones) {
            if (promocion.fechaInicio() != null && promocion.fechaFin() != null
                    && promocion.fechaFin().isBefore(promocion.fechaInicio())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha final de una promoción no puede ser anterior a su inicio");
            }
            TipoDescuentoPromocion tipo = parsearTipo(promocion.tipoDescuento());
            if (tipo == TipoDescuentoPromocion.PORCENTAJE
                    && promocion.valorDescuento().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El porcentaje de descuento no puede superar 100");
            }
            if (!serviciosValidos.containsAll(ids(promocion.servicioIds()))
                    || !sucursalesValidas.containsAll(ids(promocion.sucursalIds()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La promoción contiene servicios o sucursales de otro tenant");
            }
        }
    }

    private void guardarRelaciones(Long empresaId, Long promocionId, PromocionSitioRequest request) {
        promocionServicioRepositorio.saveAll(ids(request.servicioIds()).stream().map(servicioId -> {
            EmpresaSitioPromocionServicioEntidad relacion = new EmpresaSitioPromocionServicioEntidad();
            relacion.setEmpresaId(empresaId); relacion.setPromocionId(promocionId); relacion.setServicioId(servicioId);
            return relacion;
        }).toList());
        promocionSucursalRepositorio.saveAll(ids(request.sucursalIds()).stream().map(sucursalId -> {
            EmpresaSitioPromocionSucursalEntidad relacion = new EmpresaSitioPromocionSucursalEntidad();
            relacion.setEmpresaId(empresaId); relacion.setPromocionId(promocionId); relacion.setSucursalId(sucursalId);
            return relacion;
        }).toList());
    }

    private List<PromocionSitioAdminResponse> mapearAdmin(Long empresaId) {
        LocalDate hoy = LocalDate.now();
        Map<Long, List<Long>> servicios = agruparServicios(empresaId);
        Map<Long, List<Long>> sucursales = agruparSucursales(empresaId);
        return promocionRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .map(promocion -> new PromocionSitioAdminResponse(
                        promocion.getId(), promocion.getTitulo(), promocion.getDescripcion(), promocion.getImagenUrl(),
                        promocion.getFechaInicio(), promocion.getFechaFin(), promocion.getTipoDescuento().name(),
                        promocion.getValorDescuento(), promocion.getMoneda(), promocion.getTextoBoton(), promocion.isActiva(),
                        vigente(promocion, hoy), promocion.getOrden(),
                        servicios.getOrDefault(promocion.getId(), List.of()),
                        sucursales.getOrDefault(promocion.getId(), List.of())
                )).toList();
    }

    private Map<Long, List<Long>> agruparServicios(Long empresaId) {
        return promocionServicioRepositorio.findByEmpresaId(empresaId).stream()
                .collect(Collectors.groupingBy(EmpresaSitioPromocionServicioEntidad::getPromocionId,
                        Collectors.mapping(EmpresaSitioPromocionServicioEntidad::getServicioId, Collectors.toList())));
    }

    private Map<Long, List<Long>> agruparSucursales(Long empresaId) {
        return promocionSucursalRepositorio.findByEmpresaId(empresaId).stream()
                .collect(Collectors.groupingBy(EmpresaSitioPromocionSucursalEntidad::getPromocionId,
                        Collectors.mapping(EmpresaSitioPromocionSucursalEntidad::getSucursalId, Collectors.toList())));
    }

    private boolean vigente(EmpresaSitioPromocionEntidad promocion, LocalDate hoy) {
        return (promocion.getFechaInicio() == null || !hoy.isBefore(promocion.getFechaInicio()))
                && (promocion.getFechaFin() == null || !hoy.isAfter(promocion.getFechaFin()));
    }

    private Set<Long> ids(List<Long> valores) {
        return valores == null ? Set.of() : valores.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private TipoDescuentoPromocion parsearTipo(String valor) {
        try {
            return TipoDescuentoPromocion.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de descuento no reconocido");
        }
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

    private String valorConFallback(String valor, String fallback) {
        String limpio = normalizar(valor);
        return limpio != null ? limpio : fallback;
    }

    private void registrarAuditoria(Long empresaId, Long usuarioActorId, String antes, String despues) {
        AuditoriaConfiguracionEmpresaEntidad auditoria = new AuditoriaConfiguracionEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setModulo("SITIO");
        auditoria.setAccion("PROMOCIONES_SITIO_ACTUALIZADAS");
        auditoria.setResumen("Se actualizaron las promociones del sitio");
        auditoria.setDetalleAntesJson(antes);
        auditoria.setDetalleDespuesJson(despues);
        auditoriaRepositorio.save(auditoria);
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException excepcion) {
            throw new IllegalStateException("No fue posible serializar las promociones", excepcion);
        }
    }
}
