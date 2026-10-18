package com.techprotech.agenda.modulos.prestadores.aplicacion;

import com.techprotech.agenda.modulos.caja.aplicacion.*;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCierreCita;
import com.techprotech.agenda.modulos.citas.aplicacion.OrigenCierreCita;
import com.techprotech.agenda.modulos.citas.api.dto.ResultadoCierreCitaResponse;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;

import java.time.Clock;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.api.dto.CitaAgendaResponse;
import com.techprotech.agenda.modulos.prestadores.api.dto.PerfilStaffResponse;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioAgendaStaff {
    private final ServicioConsultaCobroCitas cobros;
    private final ServicioCierreCita cierres;
    private final Clock reloj;
    private final ZonaNegocio zonasNegocio;

    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;

    public ServicioAgendaStaff(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            Clock reloj,
            ZonaNegocio zonasNegocio,
            ServicioConsultaCobroCitas cobros,
            ServicioCierreCita cierres
    ) {
        this.cobros=cobros;
        this.cierres=cierres;
        this.reloj = reloj;
        this.zonasNegocio = zonasNegocio;
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
    }

    @Transactional(readOnly = true)
    public PerfilStaffResponse obtenerPerfil(Long empresaId, Long prestadorId) {
        PrestadorServicioEntidad prestador = prestadorServicioRepositorio.findById(prestadorId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El perfil de staff no existe"));

        SucursalEntidad sucursal = sucursalRepositorio.findById(prestador.getSucursalId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal del staff no existe"));
        if (!empresaId.equals(sucursal.getEmpresaId())) {
            throw new ResponseStatusException(NOT_FOUND, "El perfil de staff no existe para la empresa autenticada");
        }

        return new PerfilStaffResponse(
                prestador.getUsuarioId(),
                prestador.getSucursalId(),
                prestador.getNombreMostrar(),
                prestador.getBiografia(),
                prestador.getFotoUrl(),
                prestador.getColorAgenda(),
                prestador.isActivo()
        );
    }

    @Transactional(readOnly = true)
    public List<CitaAgendaResponse> obtenerAgenda(UsuarioAutenticado usuario, LocalDate desde, LocalDate hasta) {
        Long empresaId=usuario.empresaId(), prestadorId=usuario.usuarioId();
        Long sucursalId = desde == null ? obtenerPerfil(empresaId, prestadorId).sucursalId() : null;
        LocalDate inicio = desde != null ? desde : LocalDate.now(reloj.withZone(zonasNegocio.de(empresaId, sucursalId)));
        LocalDate fin = hasta != null ? hasta : inicio.plusDays(7);
        var zonasPorSucursal = new java.util.HashMap<Long, ZoneId>();
        var citas=citaRepositorio.findByEmpresaIdAndPrestadorIdAndInicioBetweenOrderByInicioAsc(
                        empresaId,
                        prestadorId,
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay()
                ).stream().filter(c -> usuario.puedeAccederSucursal(c.getSucursalId())).toList();
        var datosCobro=cobros.consultar(usuario,citas);
        var elegibles=cierres.elegibilidad(citas);
        return citas.stream()
                .map(cita -> mapear(cita,zonasPorSucursal.computeIfAbsent(cita.getSucursalId(),
                        id -> zonasNegocio.de(empresaId,id)),datosCobro.get(cita.getId()),elegibles.get(cita.getId())))
                .toList();
    }

    @Transactional
    public ResultadoCierreCitaResponse cambiarEstado(UsuarioAutenticado usuario, Long citaId, String nuevoEstado) {
        if (List.of("FINALIZADA","NO_ASISTIO").contains(nuevoEstado))
            return cobros.respuesta(usuario,cierres.cerrar(usuario,citaId,nuevoEstado,OrigenCierreCita.STAFF));
        Long empresaId=usuario.empresaId(), prestadorId=usuario.usuarioId();
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaIdAndPrestadorId(citaId, empresaId, prestadorId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para el staff autenticado"));

        if (!List.of("CONFIRMADA", "FINALIZADA", "NO_ASISTIO").contains(nuevoEstado)) {
            throw new ResponseStatusException(BAD_REQUEST, "El estado solicitado no es valido para staff");
        }

        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "La cita ya no se puede gestionar desde staff en su estado actual");
        }

        if ("CONFIRMADA".equals(nuevoEstado) && !"PENDIENTE".equals(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo puedes confirmar citas pendientes");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado(nuevoEstado);
        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setCambiadoPorUsuarioId(prestadorId);
        historial.setMotivo("Cambio de estado realizado por staff");
        historialEstadoCitaRepositorio.save(historial);
        return null;
    }

    private CitaAgendaResponse mapear(CitaEntidad cita, ZoneId zonaHoraria,CobroCita cobro,boolean puedeCerrar) {
        String sucursalNombre = sucursalRepositorio.findById(cita.getSucursalId()).map(SucursalEntidad::getNombre)
          .orElse("Sucursal");
        String servicioNombre = servicioRepositorio.findById(cita.getServicioId()).map(ServicioEntidad::getNombre)
          .orElse("Servicio");
        String clienteNombre = clienteRepositorio.findById(cita.getClienteId()).map(ClienteEntidad::getNombreCompleto)
          .orElse("Cliente");
        String clienteTelefono = clienteRepositorio.findById(cita.getClienteId()).map(ClienteEntidad::getTelefono)
          .orElse("");
        String clienteCorreo = usuarioRepositorio.findById(cita.getClienteId()).map(UsuarioEntidad::getCorreo)
          .orElse("");

        return new CitaAgendaResponse(
                cita.getId(),
                cita.getEstado(),
                sucursalNombre,
                servicioNombre,
                clienteNombre,
                clienteCorreo,
                clienteTelefono,
                cita.getInicio().atZone(zonaHoraria).toOffsetDateTime(),
                cita.getFin().atZone(zonaHoraria).toOffsetDateTime(),
                cita.getNotas(), cita.getSucursalId(),
                cobro.saldo().pagado(), cobro.saldo().condonado(), cobro.saldo().pendiente(), cobro.saldo().estadoCobro(), cobro.puedeCobrar(), puedeCerrar, cita.getMoneda(), cita.getFolio(), cobro.saldo().cobroPointEnCurso()
        );
    }
}
