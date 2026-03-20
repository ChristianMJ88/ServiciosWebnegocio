package com.techprotech.agenda.modulos.prestadores.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.api.dto.CitaAgendaResponse;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
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

    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioAgendaStaff(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio
    ) {
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Transactional(readOnly = true)
    public List<CitaAgendaResponse> obtenerAgenda(Long empresaId, Long prestadorId, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now();
        LocalDate fin = hasta != null ? hasta : inicio.plusDays(7);
        return citaRepositorio.findByEmpresaIdAndPrestadorIdAndInicioBetweenOrderByInicioAsc(
                        empresaId,
                        prestadorId,
                        inicio.atStartOfDay(),
                        fin.plusDays(1).atStartOfDay()
                )
                .stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional
    public void cambiarEstado(Long empresaId, Long prestadorId, Long citaId, String nuevoEstado) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaIdAndPrestadorId(citaId, empresaId, prestadorId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para el staff autenticado"));

        if (!List.of("CONFIRMADA", "FINALIZADA", "NO_ASISTIO").contains(nuevoEstado)) {
            throw new ResponseStatusException(BAD_REQUEST, "El estado solicitado no es valido para staff");
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
    }

    private CitaAgendaResponse mapear(CitaEntidad cita) {
        String sucursalNombre = sucursalRepositorio.findById(cita.getSucursalId()).map(s -> s.getNombre()).orElse("Sucursal");
        String servicioNombre = servicioRepositorio.findById(cita.getServicioId()).map(s -> s.getNombre()).orElse("Servicio");
        String zonaHoraria = sucursalRepositorio.findById(cita.getSucursalId()).map(s -> s.getZonaHoraria()).orElse("America/Mexico_City");
        String clienteNombre = clienteRepositorio.findById(cita.getClienteId()).map(c -> c.getNombreCompleto()).orElse("Cliente");
        String clienteTelefono = clienteRepositorio.findById(cita.getClienteId()).map(c -> c.getTelefono()).orElse("");
        String clienteCorreo = usuarioRepositorio.findById(cita.getClienteId()).map(u -> u.getCorreo()).orElse("");

        return new CitaAgendaResponse(
                cita.getId(),
                cita.getEstado(),
                sucursalNombre,
                servicioNombre,
                clienteNombre,
                clienteCorreo,
                clienteTelefono,
                cita.getInicio().atZone(ZoneId.of(zonaHoraria)).toOffsetDateTime(),
                cita.getFin().atZone(ZoneId.of(zonaHoraria)).toOffsetDateTime(),
                cita.getNotas()
        );
    }
}

