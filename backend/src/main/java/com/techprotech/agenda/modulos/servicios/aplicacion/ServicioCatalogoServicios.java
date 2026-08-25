package com.techprotech.agenda.modulos.servicios.aplicacion;

import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.GrupoServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioSucursalEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.SubgrupoServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.AsignacionServicioPrestadorRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.GrupoServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioSucursalRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.SubgrupoServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioCatalogoServicios {

    private final ServicioRepositorio servicioRepositorio;
    private final ServicioSucursalRepositorio servicioSucursalRepositorio;
    private final GrupoServicioRepositorio grupoServicioRepositorio;
    private final SubgrupoServicioRepositorio subgrupoServicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio;

    public ServicioCatalogoServicios(
            ServicioRepositorio servicioRepositorio,
            ServicioSucursalRepositorio servicioSucursalRepositorio,
            GrupoServicioRepositorio grupoServicioRepositorio,
            SubgrupoServicioRepositorio subgrupoServicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            EmpresaSitioConfigRepositorio empresaSitioConfigRepositorio
    ) {
        this.servicioRepositorio = servicioRepositorio;
        this.servicioSucursalRepositorio = servicioSucursalRepositorio;
        this.grupoServicioRepositorio = grupoServicioRepositorio;
        this.subgrupoServicioRepositorio = subgrupoServicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.empresaSitioConfigRepositorio = empresaSitioConfigRepositorio;
    }

    public List<ServicioPublicoResponse> listarPorSucursal(Long sucursalId) {
        Long sucursal = sucursalId != null ? sucursalId : 1L;
        return servicioRepositorio.findBySucursalIdAndActivoTrueOrderByOrdenPublicoAscNombreAsc(sucursal)
                .stream()
                .map(servicio -> mapearServicio(servicio, null, null, sucursal))
                .toList();
    }

    public CatalogoServiciosPublicoResponse obtenerCatalogoPorSlug(String slug) {
        EmpresaSitioConfigEntidad sitio = empresaSitioConfigRepositorio.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe un sitio publicado para ese tenant"));

        Long empresaId = sitio.getEmpresaId();
        List<GrupoServicioEntidad> grupos = grupoServicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId)
                .stream()
                .filter(GrupoServicioEntidad::isActivo)
                .toList();
        Map<Long, GrupoServicioEntidad> gruposPorId = grupos.stream()
                .collect(java.util.stream.Collectors.toMap(GrupoServicioEntidad::getId, grupo -> grupo));

        List<SubgrupoServicioEntidad> subgrupos = subgrupoServicioRepositorio.findByEmpresaIdOrderByOrdenPublicoAscNombreAsc(empresaId)
                .stream()
                .filter(SubgrupoServicioEntidad::isActivo)
                .toList();
        Map<Long, SubgrupoServicioEntidad> subgruposPorId = subgrupos.stream()
                .collect(java.util.stream.Collectors.toMap(SubgrupoServicioEntidad::getId, subgrupo -> subgrupo));

        Map<Long, List<SubgrupoServicioPublicoResponse>> subgruposPorGrupo = new LinkedHashMap<>();
        for (GrupoServicioEntidad grupo : grupos) {
            subgruposPorGrupo.put(grupo.getId(), new ArrayList<>());
        }

        Map<Long, List<ServicioPublicoResponse>> serviciosPorSubgrupo = new LinkedHashMap<>();
        List<ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdAndVisiblePublicoTrueAndActivoTrueOrderByOrdenPublicoAscNombreAsc(empresaId);
        Map<Long, List<ServicioSucursalEntidad>> sucursalesPorServicio = servicioSucursalRepositorio
                .findByEmpresaIdAndIdServicioIdInAndActivoTrue(
                        empresaId,
                        servicios.stream().map(ServicioEntidad::getId).toList()
                ).stream()
                .collect(java.util.stream.Collectors.groupingBy(asignacion -> asignacion.getId().getServicioId()));

        List<ServicioPublicoResponse> serviciosSinAgrupar = new ArrayList<>();
        for (ServicioEntidad servicio : servicios) {
            GrupoServicioEntidad grupo = servicio.getGrupoId() != null ? gruposPorId.get(servicio.getGrupoId()) : null;
            SubgrupoServicioEntidad subgrupo = servicio.getSubgrupoId() != null ? subgruposPorId.get(servicio.getSubgrupoId()) : null;
            ServicioPublicoResponse response = mapearServicio(
                    servicio,
                    grupo,
                    subgrupo,
                    resolverSucursalPrincipal(servicio, sucursalesPorServicio.get(servicio.getId()))
            );
            if (subgrupo != null) {
                serviciosPorSubgrupo.computeIfAbsent(subgrupo.getId(), ignored -> new ArrayList<>()).add(response);
                continue;
            }
            serviciosSinAgrupar.add(response);
        }

        for (SubgrupoServicioEntidad subgrupo : subgrupos) {
            GrupoServicioEntidad grupo = gruposPorId.get(subgrupo.getGrupoId());
            if (grupo == null) {
                continue;
            }
            subgruposPorGrupo.computeIfAbsent(grupo.getId(), ignored -> new ArrayList<>())
                    .add(new SubgrupoServicioPublicoResponse(
                            subgrupo.getId(),
                            subgrupo.getNombre(),
                            subgrupo.getDescripcion(),
                            subgrupo.getOrdenPublico(),
                            serviciosPorSubgrupo.getOrDefault(subgrupo.getId(), List.of())
                    ));
        }

        if (!serviciosSinAgrupar.isEmpty()) {
            GrupoServicioEntidad grupoFallback = grupos.stream().findFirst().orElse(null);
            if (grupoFallback != null) {
                subgruposPorGrupo.computeIfAbsent(grupoFallback.getId(), ignored -> new ArrayList<>())
                        .add(new SubgrupoServicioPublicoResponse(
                                -1L,
                                "Otros servicios",
                                null,
                                Integer.MAX_VALUE,
                                serviciosSinAgrupar
                        ));
            }
        }

        List<GrupoServicioPublicoResponse> gruposResponse = grupos.stream()
                .map(grupo -> new GrupoServicioPublicoResponse(
                        grupo.getId(),
                        grupo.getNombre(),
                        grupo.getDescripcion(),
                        grupo.getImagenUrl(),
                        grupo.getIcono(),
                        grupo.getOrdenPublico(),
                        subgruposPorGrupo.getOrDefault(grupo.getId(), List.of()).stream()
                                .sorted(Comparator.comparingInt(SubgrupoServicioPublicoResponse::ordenPublico))
                                .toList()
                ))
                .toList();

        if (gruposResponse.isEmpty() && !servicios.isEmpty()) {
            gruposResponse = List.of(new GrupoServicioPublicoResponse(
                    -1L,
                    "Servicios",
                    "Catálogo disponible para este negocio.",
                    sitio.getLogoUrl(),
                    null,
                    0,
                    List.of(new SubgrupoServicioPublicoResponse(
                            -1L,
                            "Disponibles",
                            null,
                            0,
                            servicios.stream()
                                    .map(servicio -> mapearServicio(
                                            servicio,
                                            servicio.getGrupoId() != null ? gruposPorId.get(servicio.getGrupoId()) : null,
                                            servicio.getSubgrupoId() != null ? subgruposPorId.get(servicio.getSubgrupoId()) : null,
                                            resolverSucursalPrincipal(servicio, sucursalesPorServicio.get(servicio.getId()))
                                    ))
                                    .toList()
                    ))
            ));
        }

        return new CatalogoServiciosPublicoResponse(
                empresaId,
                sitio.getSlug(),
                sitio.getNombreComercial(),
                gruposResponse
        );
    }

    public List<PrestadorPublicoResponse> listarPrestadores(Long empresaId, Long sucursalId, Long servicioId) {
        Long sucursalObjetivo = sucursalId != null ? sucursalId : 1L;
        final Set<Long> prestadoresHabilitados;

        if (servicioId != null) {
            servicioRepositorio.findByIdAndEmpresaIdAndActivoTrue(servicioId, empresaId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo"));

            prestadoresHabilitados = new HashSet<>();
            for (AsignacionServicioPrestadorEntidad asignacion : asignacionServicioPrestadorRepositorio.findByIdServicioIdAndActivaTrue(servicioId)) {
                prestadoresHabilitados.add(asignacion.getId().getPrestadorId());
            }
        } else {
            prestadoresHabilitados = null;
        }

        return prestadorServicioRepositorio.findBySucursalIdInOrderByNombreMostrarAsc(List.of(sucursalObjetivo))
                .stream()
                .filter(PrestadorServicioEntidad::isActivo)
                .filter(prestador -> prestadoresHabilitados == null || prestadoresHabilitados.contains(prestador.getUsuarioId()))
                .map(prestador -> new PrestadorPublicoResponse(
                        prestador.getUsuarioId(),
                        prestador.getSucursalId(),
                        prestador.getNombreMostrar(),
                        prestador.getBiografia(),
                        prestador.getColorAgenda()
                ))
                .toList();
    }

    private ServicioPublicoResponse mapearServicio(
            ServicioEntidad servicio,
            GrupoServicioEntidad grupo,
            SubgrupoServicioEntidad subgrupo,
            Long sucursalId
    ) {
        return new ServicioPublicoResponse(
                servicio.getId(),
                sucursalId,
                servicio.getGrupoId(),
                grupo != null ? grupo.getNombre() : null,
                servicio.getSubgrupoId(),
                subgrupo != null ? subgrupo.getNombre() : null,
                servicio.getNombre(),
                servicio.getSlug(),
                servicio.getDescripcion(),
                servicio.getImagenUrl(),
                servicio.getDuracionMinutos(),
                servicio.getBufferAntesMinutos(),
                servicio.getBufferDespuesMinutos(),
                servicio.getPrecio(),
                servicio.getMoneda(),
                servicio.getOrdenPublico(),
                servicio.isVisiblePublico(),
                servicio.isRequiereAnticipo(),
                servicio.getAnticipoTipo(),
                servicio.getAnticipoValor()
        );
    }

    private Long resolverSucursalPrincipal(ServicioEntidad servicio, List<ServicioSucursalEntidad> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return servicio.getSucursalId();
        }
        if (servicio.getSucursalId() != null) {
            boolean coincide = asignaciones.stream()
                    .anyMatch(asignacion -> servicio.getSucursalId().equals(asignacion.getId().getSucursalId()));
            if (coincide) {
                return servicio.getSucursalId();
            }
        }
        return asignaciones.stream()
                .map(asignacion -> asignacion.getId().getSucursalId())
                .sorted()
                .findFirst()
                .orElse(servicio.getSucursalId());
    }
}
