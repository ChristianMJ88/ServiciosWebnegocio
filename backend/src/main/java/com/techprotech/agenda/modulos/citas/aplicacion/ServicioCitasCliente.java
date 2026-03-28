package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.FranjaDisponibleResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioCitasCliente {

    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas;

    public ServicioCitasCliente(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas
    ) {
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
    }

    @Transactional(readOnly = true)
    public List<CitaClienteResponse> listarMisCitas(Long empresaId, Long clienteId) {
        return citaRepositorio.findByEmpresaIdAndClienteIdOrderByInicioDesc(empresaId, clienteId)
                .stream()
                .map(this::mapearCita)
                .toList();
    }

    @Transactional
    public CitaClienteResponse confirmar(Long empresaId, Long clienteId, Long citaId) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaIdAndClienteId(citaId, empresaId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para el cliente autenticado"));

        if (!"PENDIENTE".equals(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden confirmar citas pendientes");
        }

        if (!cita.getInicio().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pueden confirmar citas pasadas o en curso");
        }

        cita.setEstado("CONFIRMADA");
        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior("PENDIENTE");
        historial.setEstadoNuevo("CONFIRMADA");
        historial.setCambiadoPorUsuarioId(clienteId);
        historial.setMotivo("Confirmacion realizada por el cliente");
        historialEstadoCitaRepositorio.save(historial);

        ClienteEntidad cliente = clienteRepositorio.findById(clienteId).orElse(null);
        if (cliente != null && cliente.isAceptaWhatsapp() && cliente.getTelefono() != null && !cliente.getTelefono().isBlank()) {
            servicioOutboxWhatsappCitas.programarCitaConfirmada(
                    empresaId,
                    cita.getId(),
                    cliente.getTelefono(),
                    cita.getInicio()
            );
        }

        return mapearCita(cita);
    }

    @Transactional
    public void cancelar(Long empresaId, Long clienteId, Long citaId) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaIdAndClienteId(citaId, empresaId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para el cliente autenticado"));

        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden cancelar citas pendientes o confirmadas");
        }

        if (!cita.getInicio().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pueden cancelar citas pasadas o en curso");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado("CANCELADA");
        cita.setCanceladaEn(LocalDateTime.now());
        cita.setMotivoCancelacion("Cancelada por cliente");
        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo("CANCELADA");
        historial.setCambiadoPorUsuarioId(clienteId);
        historial.setMotivo("Cancelacion realizada por el cliente");
        historialEstadoCitaRepositorio.save(historial);
    }

    @Transactional
    public CitaClienteResponse reprogramar(Long empresaId, Long clienteId, Long citaId, OffsetDateTime nuevoInicio) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaIdAndClienteId(citaId, empresaId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para el cliente autenticado"));

        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden reprogramar citas pendientes o confirmadas");
        }

        if (!cita.getInicio().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pueden reprogramar citas pasadas o en curso");
        }

        String zonaHoraria = sucursalRepositorio.findById(cita.getSucursalId())
                .map(sucursal -> sucursal.getZonaHoraria())
                .orElse("America/Mexico_City");
        ZoneId zona = ZoneId.of(zonaHoraria);
        OffsetDateTime nuevoInicioZonado = nuevoInicio.atZoneSameInstant(zona).toOffsetDateTime();

        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                empresaId,
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                nuevoInicioZonado.toLocalDate()
        );

        FranjaDisponibleResponse franja = franjas.stream()
                .filter(item -> item.inicio().equals(nuevoInicioZonado.toString()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "La nueva franja ya no esta disponible"));

        String estadoAnterior = cita.getEstado();
        cita.setInicio(OffsetDateTime.parse(franja.inicio()).toLocalDateTime());
        cita.setFin(OffsetDateTime.parse(franja.fin()).toLocalDateTime());
        cita.setEstado("PENDIENTE");
        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo("PENDIENTE");
        historial.setCambiadoPorUsuarioId(clienteId);
        historial.setMotivo("Reprogramacion realizada por el cliente");
        historialEstadoCitaRepositorio.save(historial);

        ClienteEntidad cliente = clienteRepositorio.findById(clienteId).orElse(null);
        if (cliente != null && cliente.isAceptaWhatsapp() && cliente.getTelefono() != null && !cliente.getTelefono().isBlank()) {
            servicioOutboxWhatsappCitas.programarCitaReprogramadaPendiente(
                    empresaId,
                    cita.getId(),
                    cliente.getTelefono(),
                    cita.getInicio()
            );
        }

        return mapearCita(cita);
    }

    private CitaClienteResponse mapearCita(CitaEntidad cita) {
        String sucursalNombre = sucursalRepositorio.findById(cita.getSucursalId())
                .map(sucursal -> sucursal.getNombre())
                .orElse("Sucursal");
        String servicioNombre = servicioRepositorio.findById(cita.getServicioId())
                .map(servicio -> servicio.getNombre())
                .orElse("Servicio");
        var prestador = prestadorServicioRepositorio.findById(cita.getPrestadorId()).orElse(null);
        String prestadorNombre = prestador != null ? prestador.getNombreMostrar() : "Prestador";
        String zonaHoraria = sucursalRepositorio.findById(cita.getSucursalId())
                .map(sucursal -> sucursal.getZonaHoraria())
                .orElse("America/Mexico_City");
        String clienteNombre = clienteRepositorio.findById(cita.getClienteId()).map(cliente -> cliente.getNombreCompleto()).orElse("Cliente");
        String clienteTelefono = clienteRepositorio.findById(cita.getClienteId()).map(cliente -> cliente.getTelefono()).orElse("");
        String clienteCorreo = usuarioRepositorio.findById(cita.getClienteId()).map(usuario -> usuario.getCorreo()).orElse("");

        boolean cancelable = List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())
                && cita.getInicio().isAfter(LocalDateTime.now());

        return new CitaClienteResponse(
                cita.getId(),
                cita.getEstado(),
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                sucursalNombre,
                servicioNombre,
                prestadorNombre,
                cita.getInicio().atZone(ZoneId.of(zonaHoraria)).toOffsetDateTime(),
                cita.getFin().atZone(ZoneId.of(zonaHoraria)).toOffsetDateTime(),
                cita.getPrecio(),
                cita.getMoneda(),
                cita.getNotas(),
                cancelable,
                clienteNombre,
                clienteCorreo,
                clienteTelefono
        );
    }
}
