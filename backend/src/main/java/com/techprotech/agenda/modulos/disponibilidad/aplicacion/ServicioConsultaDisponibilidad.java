package com.techprotech.agenda.modulos.disponibilidad.aplicacion;

import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ExcepcionDisponibilidadEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.ExcepcionDisponibilidadRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.ReglaDisponibilidadRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.AsignacionServicioPrestadorRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioConsultaDisponibilidad {

    private static final List<String> ESTADOS_OCUPADOS = List.of("PENDIENTE", "CONFIRMADA");

    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final ReglaDisponibilidadRepositorio reglaDisponibilidadRepositorio;
    private final ExcepcionDisponibilidadRepositorio excepcionDisponibilidadRepositorio;
    private final CitaRepositorio citaRepositorio;

    public ServicioConsultaDisponibilidad(
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            ReglaDisponibilidadRepositorio reglaDisponibilidadRepositorio,
            ExcepcionDisponibilidadRepositorio excepcionDisponibilidadRepositorio,
            CitaRepositorio citaRepositorio
    ) {
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.reglaDisponibilidadRepositorio = reglaDisponibilidadRepositorio;
        this.excepcionDisponibilidadRepositorio = excepcionDisponibilidadRepositorio;
        this.citaRepositorio = citaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<FranjaDisponibleResponse> obtenerFranjasDisponibles(
            Long empresaId,
            Long sucursalId,
            Long servicioId,
            Long prestadorId,
            LocalDate fecha
    ) {
        Long empresa = empresaId != null ? empresaId : 1L;
        if (fecha == null) {
            throw new ResponseStatusException(BAD_REQUEST, "La fecha es obligatoria");
        }

        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(sucursalId, empresa)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe o no esta activa"));

        ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaIdAndSucursalIdAndActivoTrue(servicioId, empresa, sucursalId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo"));

        PrestadorServicioEntidad prestador = resolverPrestador(prestadorId, sucursalId, servicioId);
        int duracionTotalMinutos = servicio.getDuracionMinutos() + servicio.getBufferAntesMinutos() + servicio.getBufferDespuesMinutos();

        List<ReglaDisponibilidadEntidad> reglasSucursal = reglaDisponibilidadRepositorio
                .findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(empresa, "SUCURSAL", sucursal.getId(), fecha.getDayOfWeek().getValue());
        List<ReglaDisponibilidadEntidad> reglasPrestador = reglaDisponibilidadRepositorio
                .findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(empresa, "PRESTADOR", prestador.getUsuarioId(), fecha.getDayOfWeek().getValue());

        if (reglasSucursal.isEmpty() || reglasPrestador.isEmpty()) {
            return List.of();
        }

        List<VentanaHorario> ventanasBase = intersectarReglas(fecha, reglasSucursal, reglasPrestador);
        List<ExcepcionDisponibilidadEntidad> excepciones = new ArrayList<>();
        excepciones.addAll(excepcionDisponibilidadRepositorio.findByEmpresaIdAndTipoSujetoAndSujetoIdAndFechaExcepcion(empresa, "SUCURSAL", sucursal.getId(), fecha));
        excepciones.addAll(excepcionDisponibilidadRepositorio.findByEmpresaIdAndTipoSujetoAndSujetoIdAndFechaExcepcion(empresa, "PRESTADOR", prestador.getUsuarioId(), fecha));

        List<VentanaHorario> ventanasDisponibles = aplicarExcepciones(ventanasBase, excepciones, fecha);
        List<VentanaHorario> ventanasOcupadas = citaRepositorio
                .buscarConflictos(
                        prestador.getUsuarioId(),
                        fecha.atStartOfDay(),
                        fecha.plusDays(1).atStartOfDay(),
                        ESTADOS_OCUPADOS
                )
                .stream()
                .map(cita -> new VentanaHorario(cita.getInicio(), cita.getFin()))
                .toList();

        ventanasDisponibles = restarVentanas(ventanasDisponibles, ventanasOcupadas);
        int intervaloMinutos = obtenerIntervalo(reglasSucursal, reglasPrestador);
        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());

        List<FranjaDisponibleResponse> respuesta = new ArrayList<>();
        for (VentanaHorario ventana : ventanasDisponibles) {
            LocalDateTime inicioCursor = ventana.inicio();
            while (!inicioCursor.plusMinutes(duracionTotalMinutos).isAfter(ventana.fin())) {
                LocalDateTime finCursor = inicioCursor.plusMinutes(duracionTotalMinutos);
                OffsetDateTime inicioZonado = inicioCursor.atZone(zona).toOffsetDateTime();
                OffsetDateTime finZonado = finCursor.atZone(zona).toOffsetDateTime();
                respuesta.add(new FranjaDisponibleResponse(
                        inicioZonado.toString(),
                        finZonado.toString(),
                        prestador.getUsuarioId(),
                        servicio.getId(),
                        sucursal.getId()
                ));
                inicioCursor = inicioCursor.plusMinutes(intervaloMinutos);
            }
        }

        return respuesta.stream()
                .sorted(Comparator.comparing(FranjaDisponibleResponse::inicio))
                .toList();
    }

    private PrestadorServicioEntidad resolverPrestador(Long prestadorId, Long sucursalId, Long servicioId) {
        if (prestadorId != null) {
            PrestadorServicioEntidad prestador = prestadorServicioRepositorio
                    .findByUsuarioIdAndSucursalIdAndActivoTrue(prestadorId, sucursalId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El prestador no existe o no pertenece a la sucursal"));

            asignacionServicioPrestadorRepositorio
                    .findByIdPrestadorIdAndIdServicioIdAndActivaTrue(prestadorId, servicioId)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El prestador no ofrece el servicio indicado"));
            return prestador;
        }

        AsignacionServicioPrestadorEntidad asignacion = asignacionServicioPrestadorRepositorio
                .findByIdServicioIdAndActivaTrue(servicioId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No hay prestadores asignados a este servicio"));

        return prestadorServicioRepositorio.findByUsuarioIdAndSucursalIdAndActivoTrue(asignacion.getId().getPrestadorId(), sucursalId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No hay prestadores activos para este servicio en la sucursal"));
    }

    private List<VentanaHorario> intersectarReglas(
            LocalDate fecha,
            List<ReglaDisponibilidadEntidad> reglasSucursal,
            List<ReglaDisponibilidadEntidad> reglasPrestador
    ) {
        List<VentanaHorario> resultado = new ArrayList<>();
        for (ReglaDisponibilidadEntidad reglaSucursal : reglasSucursal) {
            if (!reglaVigente(reglaSucursal, fecha)) {
                continue;
            }
            for (ReglaDisponibilidadEntidad reglaPrestador : reglasPrestador) {
                if (!reglaVigente(reglaPrestador, fecha)) {
                    continue;
                }

                LocalTime inicio = reglaSucursal.getHoraInicio().isAfter(reglaPrestador.getHoraInicio())
                        ? reglaSucursal.getHoraInicio()
                        : reglaPrestador.getHoraInicio();
                LocalTime fin = reglaSucursal.getHoraFin().isBefore(reglaPrestador.getHoraFin())
                        ? reglaSucursal.getHoraFin()
                        : reglaPrestador.getHoraFin();

                if (inicio.isBefore(fin)) {
                    resultado.add(new VentanaHorario(fecha.atTime(inicio), fecha.atTime(fin)));
                }
            }
        }
        return resultado;
    }

    private boolean reglaVigente(ReglaDisponibilidadEntidad regla, LocalDate fecha) {
        boolean desdeOk = regla.getVigenteDesde() == null || !fecha.isBefore(regla.getVigenteDesde());
        boolean hastaOk = regla.getVigenteHasta() == null || !fecha.isAfter(regla.getVigenteHasta());
        return desdeOk && hastaOk;
    }

    private List<VentanaHorario> aplicarExcepciones(
            List<VentanaHorario> ventanas,
            List<ExcepcionDisponibilidadEntidad> excepciones,
            LocalDate fecha
    ) {
        List<VentanaHorario> resultado = new ArrayList<>(ventanas);
        for (ExcepcionDisponibilidadEntidad excepcion : excepciones) {
            LocalDateTime inicio = excepcion.getHoraInicio() != null ? fecha.atTime(excepcion.getHoraInicio()) : fecha.atStartOfDay();
            LocalDateTime fin = excepcion.getHoraFin() != null ? fecha.atTime(excepcion.getHoraFin()) : fecha.plusDays(1).atStartOfDay();
            resultado = restarVentanas(resultado, List.of(new VentanaHorario(inicio, fin)));
        }
        return resultado;
    }

    private List<VentanaHorario> restarVentanas(List<VentanaHorario> base, List<VentanaHorario> restas) {
        List<VentanaHorario> resultado = new ArrayList<>(base);
        for (VentanaHorario resta : restas) {
            List<VentanaHorario> temporal = new ArrayList<>();
            for (VentanaHorario ventana : resultado) {
                temporal.addAll(restarVentana(ventana, resta));
            }
            resultado = temporal;
        }
        return resultado;
    }

    private List<VentanaHorario> restarVentana(VentanaHorario base, VentanaHorario resta) {
        if (!base.seTraslapa(resta)) {
            return List.of(base);
        }

        List<VentanaHorario> resultado = new ArrayList<>();
        if (resta.inicio().isAfter(base.inicio())) {
            resultado.add(new VentanaHorario(base.inicio(), resta.inicio()));
        }
        if (resta.fin().isBefore(base.fin())) {
            resultado.add(new VentanaHorario(resta.fin(), base.fin()));
        }
        return resultado.stream().filter(VentanaHorario::esValida).toList();
    }

    private int obtenerIntervalo(List<ReglaDisponibilidadEntidad> reglasSucursal, List<ReglaDisponibilidadEntidad> reglasPrestador) {
        int intervaloSucursal = reglasSucursal.stream().map(ReglaDisponibilidadEntidad::getIntervaloMinutos).min(Integer::compareTo).orElse(15);
        int intervaloPrestador = reglasPrestador.stream().map(ReglaDisponibilidadEntidad::getIntervaloMinutos).min(Integer::compareTo).orElse(15);
        return Math.max(intervaloSucursal, intervaloPrestador);
    }

    private record VentanaHorario(LocalDateTime inicio, LocalDateTime fin) {
        boolean seTraslapa(VentanaHorario otra) {
            return inicio.isBefore(otra.fin) && fin.isAfter(otra.inicio);
        }

        boolean esValida() {
            return inicio.isBefore(fin);
        }
    }
}
