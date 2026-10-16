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
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioSucursalRepositorio;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioConsultaDisponibilidad {

    private static final List<String> ESTADOS_OCUPADOS = List.of("PENDIENTE", "PENDIENTE_PAGO", "CONFIRMADA");

    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ServicioSucursalRepositorio servicioSucursalRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final ReglaDisponibilidadRepositorio reglaDisponibilidadRepositorio;
    private final ExcepcionDisponibilidadRepositorio excepcionDisponibilidadRepositorio;
    private final CitaRepositorio citaRepositorio;
    private final ServicioConfiguracionReservas configuracion;
    private final java.time.Clock reloj;

    public ServicioConsultaDisponibilidad(SucursalRepositorio sucursalRepositorio, ServicioRepositorio servicioRepositorio,
        ServicioSucursalRepositorio servicioSucursalRepositorio,PrestadorServicioRepositorio prestadorServicioRepositorio,
        AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,ReglaDisponibilidadRepositorio reglaDisponibilidadRepositorio,
        ExcepcionDisponibilidadRepositorio excepcionDisponibilidadRepositorio,CitaRepositorio citaRepositorio,
        ServicioConfiguracionReservas configuracion,java.time.Clock reloj) {
    this.configuracion=configuracion; this.reloj=reloj;
    this.sucursalRepositorio = sucursalRepositorio;

        this.servicioRepositorio = servicioRepositorio;
        this.servicioSucursalRepositorio = servicioSucursalRepositorio;
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
        if (empresaId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "La empresa es obligatoria");
        }
        Long empresa = empresaId;
        if (fecha == null) {
            throw new ResponseStatusException(BAD_REQUEST, "La fecha es obligatoria");
        }

        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(sucursalId, empresa)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe o no esta activa"));

        ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaIdAndActivoTrue(servicioId, empresa)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo"));
        if (!servicioSucursalRepositorio.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(empresa, servicioId, sucursalId)) {
            throw new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo en la sucursal");
        }

var politica=configuracion.obtener(empresa);
LocalDate hoy=LocalDate.now(reloj.withZone(ZoneId.of(sucursal.getZonaHoraria())));
if(fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(politica.ventanaReservaDias()))) return List.of();

if (prestadorId != null) {

            PrestadorServicioEntidad prestador = resolverPrestador(empresa,prestadorId, sucursalId, servicioId);
            return calcularFranjasParaPrestador(empresa, sucursal, servicio, prestador, fecha, null, LocalDateTime.now(reloj.withZone(ZoneId.of(sucursal.getZonaHoraria()))).plusMinutes(politica.anticipacionMinimaMinutos()), false);
        }

        List<PrestadorServicioEntidad> prestadores = resolverPrestadoresDisponibles(empresa,sucursalId, servicioId);
        List<FranjaDisponibleResponse> respuesta = new ArrayList<>();
        for (PrestadorServicioEntidad prestador : prestadores) {
            respuesta.addAll(calcularFranjasParaPrestador(empresa, sucursal, servicio, prestador, fecha, null, LocalDateTime.now(reloj.withZone(ZoneId.of(sucursal.getZonaHoraria()))).plusMinutes(politica.anticipacionMinimaMinutos()), false));
        }

        return respuesta.stream()
                .sorted(Comparator.comparing(FranjaDisponibleResponse::inicio).thenComparing(FranjaDisponibleResponse::prestadorId))
                .toList();
    }

@Transactional(readOnly = true)
public DiasDisponiblesResponse obtenerDias(Long empresaId, Long sucursalId, List<Long> servicios,
        List<Long> elegidos, LocalDate desde, LocalDate hasta) {
    var sucursal = sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(sucursalId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no está disponible"));
    var politica = configuracion.obtener(empresaId);
    var zona = ZoneId.of(sucursal.getZonaHoraria());
    LocalDate hoy = LocalDate.now(reloj.withZone(zona));
    LocalDate limite = hoy.plusDays(politica.ventanaReservaDias());
    LocalDate inicio = desde == null ? hoy : desde;
    LocalDate fin = hasta == null ? inicio.plusDays(30) : hasta;
    if (servicios == null || servicios.isEmpty() || servicios.size() > 20 || fin.isBefore(inicio)
            || java.time.temporal.ChronoUnit.DAYS.between(inicio, fin) > 30)
        throw new ResponseStatusException(BAD_REQUEST, "Indica servicios y un rango máximo de 31 días");
    if (elegidos != null && elegidos.size() != servicios.size())
        throw new ResponseStatusException(BAD_REQUEST, "Los prestadores deben corresponder a los servicios");
    if (inicio.isBefore(hoy)) inicio = hoy;
    if (fin.isAfter(limite)) fin = limite;
    if (fin.isBefore(inicio)) return new DiasDisponiblesResponse(hoy, limite, List.of(), politica);
    var catalogo = new java.util.LinkedHashMap<Long, ServicioEntidad>();
    var candidatos = new java.util.LinkedHashMap<Long, List<PrestadorServicioEntidad>>();
    var todos = prestadorServicioRepositorio.findPublicosPorEmpresaYSucursal(empresaId, sucursalId);
    for (int i = 0; i < servicios.size(); i++) {
        Long id = servicios.get(i);
        var servicio = servicioRepositorio.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Servicio no disponible"));
        if (!servicioSucursalRepositorio.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(empresaId, id, sucursalId))
            throw new ResponseStatusException(NOT_FOUND, "Servicio no disponible en la sucursal");
        Set<Long> asignados = asignacionServicioPrestadorRepositorio.findActivasPorEmpresaYServicio(empresaId, id)
                .stream().map(a -> a.getId().getPrestadorId()).collect(Collectors.toSet());
        Long elegido = elegidos == null ? null : elegidos.get(i);
        var disponibles = todos.stream().filter(p -> asignados.contains(p.getUsuarioId())
                && (elegido == null || elegido == 0 || elegido.equals(p.getUsuarioId()))).toList();
        if (elegido != null && elegido != 0 && disponibles.isEmpty())
            throw new ResponseStatusException(NOT_FOUND, "El profesional no está disponible para este servicio");
        if (disponibles.isEmpty()) return new DiasDisponiblesResponse(hoy, limite, List.of(), politica);
        catalogo.put(id, servicio); candidatos.put((long)i, disponibles);
    }
    var ids = candidatos.values().stream().flatMap(List::stream).map(PrestadorServicioEntidad::getUsuarioId).distinct().toList();
    var contexto = new ContextoRango(
            reglaDisponibilidadRepositorio.buscarParaRango(empresaId, sucursalId, ids),
            excepcionDisponibilidadRepositorio.buscarParaRango(empresaId, sucursalId, ids, inicio, fin),
            citaRepositorio.buscarOcupadasRango(empresaId, ids, inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(), ESTADOS_OCUPADOS));
    LocalDateTime corte = LocalDateTime.now(reloj.withZone(zona)).plusMinutes(politica.anticipacionMinimaMinutos());
    List<LocalDate> dias = new ArrayList<>();
    for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
        boolean disponible = true;
        for (int posicion = 0; posicion < servicios.size(); posicion++) {
            Long id = servicios.get(posicion);
            boolean alguno = false;
            for (var prestador : candidatos.get((long)posicion)) {
                if (!calcularFranjasParaPrestador(empresaId, sucursal, catalogo.get(id), prestador, dia, contexto, corte, true).isEmpty()) {
                    alguno = true; break;
                }
            }
            if (!alguno) { disponible = false; break; }
        }
        if (disponible) dias.add(dia);
    }
    return new DiasDisponiblesResponse(hoy, limite, List.copyOf(dias), politica);
}

private record ContextoRango(List<ReglaDisponibilidadEntidad> reglas,
        List<ExcepcionDisponibilidadEntidad> excepciones,
        List<com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad> citas) {}

    private List<FranjaDisponibleResponse> calcularFranjasParaPrestador(
            Long empresa,
            SucursalEntidad sucursal,
            ServicioEntidad servicio,
            PrestadorServicioEntidad prestador,
            LocalDate fecha, ContextoRango contexto, LocalDateTime corte, boolean primera
    ) {
        int duracionTotalMinutos = servicio.getDuracionMinutos() + servicio.getBufferAntesMinutos() + servicio.getBufferDespuesMinutos();

        List<ReglaDisponibilidadEntidad> reglasSucursal = contexto == null ? reglaDisponibilidadRepositorio
                .findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(empresa, "SUCURSAL", sucursal.getId(), fecha.getDayOfWeek().getValue()) : contexto.reglas().stream().filter(r -> "SUCURSAL".equals(r.getTipoSujeto()) && sucursal.getId().equals(r.getSujetoId()) && r.getDiaSemana() == fecha.getDayOfWeek().getValue()).toList();
        List<ReglaDisponibilidadEntidad> reglasPrestador = contexto == null ? reglaDisponibilidadRepositorio
                .findByEmpresaIdAndTipoSujetoAndSujetoIdAndDiaSemana(empresa, "PRESTADOR", prestador.getUsuarioId(), fecha.getDayOfWeek().getValue()) : contexto.reglas().stream().filter(r -> "PRESTADOR".equals(r.getTipoSujeto()) && prestador.getUsuarioId().equals(r.getSujetoId()) && r.getDiaSemana() == fecha.getDayOfWeek().getValue()).toList();

        if (reglasSucursal.isEmpty() || reglasPrestador.isEmpty()) {
            return List.of();
        }

        List<VentanaHorario> ventanasBase = intersectarReglas(fecha, reglasSucursal, reglasPrestador);
        List<ExcepcionDisponibilidadEntidad> excepciones = new ArrayList<>();
if (contexto == null) {
    excepciones.addAll(excepcionDisponibilidadRepositorio.findByEmpresaIdAndTipoSujetoAndSujetoIdAndFechaExcepcion(empresa, "SUCURSAL", sucursal.getId(), fecha));
    excepciones.addAll(excepcionDisponibilidadRepositorio.findByEmpresaIdAndTipoSujetoAndSujetoIdAndFechaExcepcion(empresa, "PRESTADOR", prestador.getUsuarioId(), fecha));
} else {
    excepciones.addAll(contexto.excepciones().stream().filter(e -> fecha.equals(e.getFechaExcepcion())
            && ("SUCURSAL".equals(e.getTipoSujeto()) && sucursal.getId().equals(e.getSujetoId())
            || "PRESTADOR".equals(e.getTipoSujeto()) && prestador.getUsuarioId().equals(e.getSujetoId()))).toList());
}

        List<VentanaHorario> ventanasDisponibles = aplicarExcepciones(ventanasBase, excepciones, fecha);
        List<VentanaHorario> ventanasOcupadas = (contexto == null ? citaRepositorio
                .buscarConflictos(
                        prestador.getUsuarioId(),
                        fecha.atStartOfDay(),
                        fecha.plusDays(1).atStartOfDay(),
                        ESTADOS_OCUPADOS
                ) : contexto.citas().stream().filter(c -> prestador.getUsuarioId().equals(c.getPrestadorId()) && c.getInicio().isBefore(fecha.plusDays(1).atStartOfDay()) && c.getFin().isAfter(fecha.atStartOfDay())).toList())
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
                if(inicioCursor.isBefore(corte)) { inicioCursor=inicioCursor.plusMinutes(intervaloMinutos); continue; }
                OffsetDateTime inicioZonado = inicioCursor.atZone(zona).toOffsetDateTime();
                OffsetDateTime finZonado = finCursor.atZone(zona).toOffsetDateTime();
                respuesta.add(new FranjaDisponibleResponse(
                        inicioZonado.toString(),
                        finZonado.toString(),
                        prestador.getUsuarioId(),
                        servicio.getId(),
                        sucursal.getId()
                ));
                if (primera) return respuesta;
                inicioCursor = inicioCursor.plusMinutes(intervaloMinutos);
            }
        }

        return respuesta;
    }

    private PrestadorServicioEntidad resolverPrestador(Long empresaId, Long prestadorId, Long sucursalId, Long servicioId) {
    return resolverPrestadoresDisponibles(empresaId,sucursalId,servicioId).stream()
            .filter(p -> prestadorId == null || prestadorId.equals(p.getUsuarioId())).findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"El prestador no está disponible para este servicio y sucursal"));
}
private List<PrestadorServicioEntidad> resolverPrestadoresDisponibles(Long empresaId, Long sucursalId, Long servicioId) {
    Set<Long> asignados=asignacionServicioPrestadorRepositorio.findActivasPorEmpresaYServicio(empresaId,servicioId).stream()
            .map(a -> a.getId().getPrestadorId()).collect(Collectors.toSet());
    return prestadorServicioRepositorio.findPublicosPorEmpresaYSucursal(empresaId,sucursalId).stream()
            .filter(p -> asignados.contains(p.getUsuarioId())).toList();
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
