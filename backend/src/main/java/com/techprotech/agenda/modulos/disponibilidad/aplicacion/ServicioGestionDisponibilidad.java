package com.techprotech.agenda.modulos.disponibilidad.aplicacion;

import com.techprotech.agenda.modulos.disponibilidad.api.dto.ExcepcionDisponibilidadRequest;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ExcepcionDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ReglaDisponibilidadRequest;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ReglaDisponibilidadResponse;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ExcepcionDisponibilidadEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.ReglaDisponibilidadEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.ExcepcionDisponibilidadRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.ReglaDisponibilidadRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioGestionDisponibilidad {

    private final ReglaDisponibilidadRepositorio reglaDisponibilidadRepositorio;
    private final ExcepcionDisponibilidadRepositorio excepcionDisponibilidadRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;

    public ServicioGestionDisponibilidad(
            ReglaDisponibilidadRepositorio reglaDisponibilidadRepositorio,
            ExcepcionDisponibilidadRepositorio excepcionDisponibilidadRepositorio,
            SucursalRepositorio sucursalRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio
    ) {
        this.reglaDisponibilidadRepositorio = reglaDisponibilidadRepositorio;
        this.excepcionDisponibilidadRepositorio = excepcionDisponibilidadRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ReglaDisponibilidadResponse> listarReglasAdmin(Long empresaId) {
        Map<String, Map<Long, String>> nombres = construirMapaSujetos(empresaId);
        return reglaDisponibilidadRepositorio.findByEmpresaIdOrderByTipoSujetoAscSujetoIdAscDiaSemanaAscHoraInicioAsc(empresaId)
                .stream()
                .map(regla -> mapearRegla(regla, nombreSujeto(nombres, regla.getTipoSujeto(), regla.getSujetoId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExcepcionDisponibilidadResponse> listarExcepcionesAdmin(Long empresaId) {
        Map<String, Map<Long, String>> nombres = construirMapaSujetos(empresaId);
        return excepcionDisponibilidadRepositorio.findByEmpresaIdOrderByFechaExcepcionDescTipoSujetoAsc(empresaId)
                .stream()
                .map(excepcion -> mapearExcepcion(excepcion, nombreSujeto(nombres, excepcion.getTipoSujeto(), excepcion.getSujetoId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReglaDisponibilidadResponse> listarReglasStaff(Long empresaId, Long prestadorId) {
        return reglaDisponibilidadRepositorio.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByDiaSemanaAscHoraInicioAsc(empresaId, "PRESTADOR", prestadorId)
                .stream()
                .map(regla -> mapearRegla(regla, "Mi agenda"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExcepcionDisponibilidadResponse> listarExcepcionesStaff(Long empresaId, Long prestadorId) {
        return excepcionDisponibilidadRepositorio.findByEmpresaIdAndTipoSujetoAndSujetoIdOrderByFechaExcepcionDescHoraInicioAsc(empresaId, "PRESTADOR", prestadorId)
                .stream()
                .map(excepcion -> mapearExcepcion(excepcion, "Mi agenda"))
                .toList();
    }

    @Transactional
    public ReglaDisponibilidadResponse crearReglaAdmin(Long empresaId, ReglaDisponibilidadRequest request) {
        String tipoSujeto = normalizarTipoSujeto(request.tipoSujeto());
        validarRegla(request);
        String nombreSujeto = validarSujeto(empresaId, tipoSujeto, request.sujetoId());

        ReglaDisponibilidadEntidad regla = new ReglaDisponibilidadEntidad();
        regla.setEmpresaId(empresaId);
        regla.setTipoSujeto(tipoSujeto);
        regla.setSujetoId(request.sujetoId());
        aplicarRegla(regla, request);
        return mapearRegla(reglaDisponibilidadRepositorio.save(regla), nombreSujeto);
    }

    @Transactional
    public ReglaDisponibilidadResponse actualizarReglaAdmin(Long empresaId, Long reglaId, ReglaDisponibilidadRequest request) {
        String tipoSujeto = normalizarTipoSujeto(request.tipoSujeto());
        validarRegla(request);
        String nombreSujeto = validarSujeto(empresaId, tipoSujeto, request.sujetoId());

        ReglaDisponibilidadEntidad regla = reglaDisponibilidadRepositorio.findByIdAndEmpresaId(reglaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La regla no existe para la empresa"));
        regla.setTipoSujeto(tipoSujeto);
        regla.setSujetoId(request.sujetoId());
        aplicarRegla(regla, request);
        return mapearRegla(reglaDisponibilidadRepositorio.save(regla), nombreSujeto);
    }

    @Transactional
    public ReglaDisponibilidadResponse guardarReglaStaff(Long empresaId, Long prestadorId, Long reglaId, ReglaDisponibilidadRequest request) {
        validarRegla(request);
        validarSujeto(empresaId, "PRESTADOR", prestadorId);

        ReglaDisponibilidadEntidad regla = reglaId == null
                ? new ReglaDisponibilidadEntidad()
                : reglaDisponibilidadRepositorio.findByIdAndEmpresaIdAndTipoSujetoAndSujetoId(reglaId, empresaId, "PRESTADOR", prestadorId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La regla no existe para el staff autenticado"));

        regla.setEmpresaId(empresaId);
        regla.setTipoSujeto("PRESTADOR");
        regla.setSujetoId(prestadorId);
        aplicarRegla(regla, request);
        return mapearRegla(reglaDisponibilidadRepositorio.save(regla), "Mi agenda");
    }

    @Transactional
    public ExcepcionDisponibilidadResponse crearExcepcionAdmin(Long empresaId, ExcepcionDisponibilidadRequest request) {
        String tipoSujeto = normalizarTipoSujeto(request.tipoSujeto());
        validarExcepcion(request);
        String nombreSujeto = validarSujeto(empresaId, tipoSujeto, request.sujetoId());

        ExcepcionDisponibilidadEntidad excepcion = new ExcepcionDisponibilidadEntidad();
        excepcion.setEmpresaId(empresaId);
        excepcion.setTipoSujeto(tipoSujeto);
        excepcion.setSujetoId(request.sujetoId());
        aplicarExcepcion(excepcion, request);
        return mapearExcepcion(excepcionDisponibilidadRepositorio.save(excepcion), nombreSujeto);
    }

    @Transactional
    public ExcepcionDisponibilidadResponse actualizarExcepcionAdmin(Long empresaId, Long excepcionId, ExcepcionDisponibilidadRequest request) {
        String tipoSujeto = normalizarTipoSujeto(request.tipoSujeto());
        validarExcepcion(request);
        String nombreSujeto = validarSujeto(empresaId, tipoSujeto, request.sujetoId());

        ExcepcionDisponibilidadEntidad excepcion = excepcionDisponibilidadRepositorio.findByIdAndEmpresaId(excepcionId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La excepcion no existe para la empresa"));
        excepcion.setTipoSujeto(tipoSujeto);
        excepcion.setSujetoId(request.sujetoId());
        aplicarExcepcion(excepcion, request);
        return mapearExcepcion(excepcionDisponibilidadRepositorio.save(excepcion), nombreSujeto);
    }

    @Transactional
    public ExcepcionDisponibilidadResponse guardarExcepcionStaff(Long empresaId, Long prestadorId, Long excepcionId, ExcepcionDisponibilidadRequest request) {
        validarExcepcion(request);
        validarSujeto(empresaId, "PRESTADOR", prestadorId);

        ExcepcionDisponibilidadEntidad excepcion = excepcionId == null
                ? new ExcepcionDisponibilidadEntidad()
                : excepcionDisponibilidadRepositorio.findByIdAndEmpresaIdAndTipoSujetoAndSujetoId(excepcionId, empresaId, "PRESTADOR", prestadorId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La excepcion no existe para el staff autenticado"));

        excepcion.setEmpresaId(empresaId);
        excepcion.setTipoSujeto("PRESTADOR");
        excepcion.setSujetoId(prestadorId);
        aplicarExcepcion(excepcion, request);
        return mapearExcepcion(excepcionDisponibilidadRepositorio.save(excepcion), "Mi agenda");
    }

    private void aplicarRegla(ReglaDisponibilidadEntidad regla, ReglaDisponibilidadRequest request) {
        regla.setDiaSemana(request.diaSemana());
        regla.setHoraInicio(request.horaInicio());
        regla.setHoraFin(request.horaFin());
        regla.setIntervaloMinutos(request.intervaloMinutos());
        regla.setVigenteDesde(request.vigenteDesde());
        regla.setVigenteHasta(request.vigenteHasta());
    }

    private void aplicarExcepcion(ExcepcionDisponibilidadEntidad excepcion, ExcepcionDisponibilidadRequest request) {
        excepcion.setFechaExcepcion(request.fechaExcepcion());
        excepcion.setHoraInicio(request.horaInicio());
        excepcion.setHoraFin(request.horaFin());
        excepcion.setTipoBloqueo(request.tipoBloqueo().trim().toUpperCase());
        excepcion.setMotivo(request.motivo() != null ? request.motivo().trim() : null);
    }

    private void validarRegla(ReglaDisponibilidadRequest request) {
        if (!request.horaInicio().isBefore(request.horaFin())) {
            throw new ResponseStatusException(BAD_REQUEST, "La hora de inicio debe ser menor a la hora de fin");
        }
        if (request.vigenteDesde() != null && request.vigenteHasta() != null && request.vigenteHasta().isBefore(request.vigenteDesde())) {
            throw new ResponseStatusException(BAD_REQUEST, "La vigencia final no puede ser menor a la inicial");
        }
    }

    private void validarExcepcion(ExcepcionDisponibilidadRequest request) {
        if ((request.horaInicio() == null) != (request.horaFin() == null)) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar ambas horas o dejar ambas vacias para un bloqueo completo");
        }
        if (request.horaInicio() != null && !request.horaInicio().isBefore(request.horaFin())) {
            throw new ResponseStatusException(BAD_REQUEST, "La hora de inicio debe ser menor a la hora de fin");
        }
    }

    private String normalizarTipoSujeto(String tipoSujeto) {
        String tipo = tipoSujeto.trim().toUpperCase();
        if (!List.of("SUCURSAL", "PRESTADOR").contains(tipo)) {
            throw new ResponseStatusException(BAD_REQUEST, "El tipo de sujeto no es valido");
        }
        return tipo;
    }

    private String validarSujeto(Long empresaId, String tipoSujeto, Long sujetoId) {
        return switch (tipoSujeto) {
            case "SUCURSAL" -> sucursalRepositorio.findByIdAndEmpresaId(sujetoId, empresaId)
                    .map(SucursalEntidad::getNombre)
                    .orElseThrow(noExiste("La sucursal no existe para la empresa"));
            case "PRESTADOR" -> {
                PrestadorServicioEntidad prestador = prestadorServicioRepositorio.findById(sujetoId)
                        .orElseThrow(noExiste("El prestador no existe para la empresa"));
                sucursalRepositorio.findByIdAndEmpresaId(prestador.getSucursalId(), empresaId)
                        .orElseThrow(noExiste("El prestador no existe para la empresa"));
                yield prestador.getNombreMostrar();
            }
            default -> throw new ResponseStatusException(BAD_REQUEST, "El tipo de sujeto no es valido");
        };
    }

    private Map<String, Map<Long, String>> construirMapaSujetos(Long empresaId) {
        Map<Long, String> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        Map<Long, String> prestadores = prestadorServicioRepositorio.findBySucursalIdInOrderByNombreMostrarAsc(sucursales.keySet()).stream()
                .collect(Collectors.toMap(PrestadorServicioEntidad::getUsuarioId, PrestadorServicioEntidad::getNombreMostrar, (a, b) -> a));
        return Map.of(
                "SUCURSAL", sucursales,
                "PRESTADOR", prestadores
        );
    }

    private String nombreSujeto(Map<String, Map<Long, String>> nombres, String tipoSujeto, Long sujetoId) {
        return nombres.getOrDefault(tipoSujeto, Map.of()).getOrDefault(sujetoId, tipoSujeto + " #" + sujetoId);
    }

    private ReglaDisponibilidadResponse mapearRegla(ReglaDisponibilidadEntidad regla, String sujetoNombre) {
        return new ReglaDisponibilidadResponse(
                regla.getId(),
                regla.getTipoSujeto(),
                regla.getSujetoId(),
                sujetoNombre,
                regla.getDiaSemana(),
                regla.getHoraInicio(),
                regla.getHoraFin(),
                regla.getIntervaloMinutos(),
                regla.getVigenteDesde(),
                regla.getVigenteHasta()
        );
    }

    private ExcepcionDisponibilidadResponse mapearExcepcion(ExcepcionDisponibilidadEntidad excepcion, String sujetoNombre) {
        return new ExcepcionDisponibilidadResponse(
                excepcion.getId(),
                excepcion.getTipoSujeto(),
                excepcion.getSujetoId(),
                sujetoNombre,
                excepcion.getFechaExcepcion(),
                excepcion.getHoraInicio(),
                excepcion.getHoraFin(),
                excepcion.getTipoBloqueo(),
                excepcion.getMotivo()
        );
    }

    private Supplier<ResponseStatusException> noExiste(String mensaje) {
        return () -> new ResponseStatusException(NOT_FOUND, mensaje);
    }
}
