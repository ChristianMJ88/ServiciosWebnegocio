package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.dominio.eventos.*;
import java.time.Clock;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.FranjaDisponibleResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
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
    private final PublicadorEventosCita eventos;
    private final Clock reloj;
    private final ZonaNegocio zonasNegocio;

    private final CitaRepositorio citaRepositorio;
    private final com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas reservas;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioCitasCliente(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            PublicadorEventosCita eventos,
            com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas reservas,
            Clock reloj,
            ZonaNegocio zonasNegocio
    ) {
        this.eventos = eventos;
        this.reloj = reloj;
        this.zonasNegocio = zonasNegocio;
        this.reservas=reservas;
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Transactional(readOnly = true)
    public List<CitaClienteResponse> listarMisCitas(Long empresaId, Long clienteId) {
        var zonasPorSucursal = new java.util.HashMap<Long, ZoneId>();
        return citaRepositorio.findByEmpresaIdAndClienteIdOrderByInicioDesc(empresaId, clienteId)
                .stream()
                .map(cita -> mapearCita(cita, zonasPorSucursal.computeIfAbsent(cita.getSucursalId(),
                        id -> zonasNegocio.de(empresaId, id))))
                .toList();
    }

    @Transactional
    public CitaClienteResponse confirmar(Long empresaId, Long clienteId, Long citaId) {
        return confirmar(empresaId, clienteId, citaId, true);
    }

    @Transactional
    public CitaClienteResponse confirmar(Long empresaId, Long clienteId, Long citaId, boolean programarWhatsappConfirmacion) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaIdAndClienteId(citaId, empresaId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para el cliente autenticado"));

        if (!"PENDIENTE".equals(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden confirmar citas pendientes");
        }

        sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(cita.getSucursalId(),empresaId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"La sucursal no está disponible"));
        var zonaConfirmacion = zonasNegocio.de(empresaId, cita.getSucursalId());
        if (!cita.getInicio().isAfter(LocalDateTime.now(reloj.withZone(zonaConfirmacion)))) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pueden confirmar citas pasadas o en curso");
        }

        if(PlazoConfirmacionCita.vencido(cita.getInicio(),zonaConfirmacion,reservas.obtener(empresaId).liberacionSinConfirmacionHoras(),cita.getCreadaEn(),reloj)) throw new ResponseStatusException(CONFLICT,"El plazo para confirmar la cita venció");
        cita.setEstado("CONFIRMADA");
        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior("PENDIENTE");
        historial.setEstadoNuevo("CONFIRMADA");
        historial.setCambiadoPorUsuarioId(clienteId);
        historial.setMotivo("Confirmacion realizada por el cliente");
        historialEstadoCitaRepositorio.save(historial);

        eventos.publicar(new CitaConfirmada(eventos.datos(cita, OrigenEventoCita.CLIENTE, clienteId),
                programarWhatsappConfirmacion, false));

        return mapearCita(cita);
    }

    @Transactional
    public void cancelar(Long empresaId, Long clienteId, Long citaId) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaIdAndClienteId(citaId, empresaId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para el cliente autenticado"));

        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden cancelar citas pendientes o confirmadas");
        }

        if (!cita.getInicio().isAfter(LocalDateTime.now(reloj.withZone(zonasNegocio.de(cita.getEmpresaId(), cita.getSucursalId()))))) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pueden cancelar citas pasadas o en curso");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado("CANCELADA");
        cita.setCanceladaEn(LocalDateTime.now(reloj));
        cita.setMotivoCancelacion("Cancelada por cliente");
        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo("CANCELADA");
        historial.setCambiadoPorUsuarioId(clienteId);
        historial.setMotivo("Cancelacion realizada por el cliente");
        historialEstadoCitaRepositorio.save(historial);
        eventos.publicar(new CitaCancelada(eventos.datos(cita, OrigenEventoCita.CLIENTE, clienteId)));
    }

    @Transactional
    public CitaClienteResponse reprogramar(Long empresaId, Long clienteId, Long citaId, OffsetDateTime nuevoInicio) {
        return reprogramar(empresaId, clienteId, citaId, nuevoInicio, OrigenEventoCita.CLIENTE, clienteId);
    }

    @Transactional
    public CitaClienteResponse reprogramar(Long empresaId, Long clienteId, Long citaId, OffsetDateTime nuevoInicio,
            OrigenEventoCita origen, Long actorId) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaIdAndClienteId(citaId, empresaId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para el cliente autenticado"));
        prestadorServicioRepositorio.bloquearPorEmpresa(empresaId, cita.getPrestadorId())
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "El prestador ya no está disponible"));

        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden reprogramar citas pendientes o confirmadas");
        }

        if (!cita.getInicio().isAfter(LocalDateTime.now(reloj.withZone(zonasNegocio.de(cita.getEmpresaId(), cita.getSucursalId()))))) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pueden reprogramar citas pasadas o en curso");
        }

        ZoneId zona = zonasNegocio.de(empresaId, cita.getSucursalId());
        OffsetDateTime nuevoInicioZonado = nuevoInicio.atZoneSameInstant(zona).toOffsetDateTime();

        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                empresaId,
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                nuevoInicioZonado.toLocalDate()
        );

        FranjaDisponibleResponse franja = franjas.stream()
                .filter(item -> coincideInicioFranja(item.inicio(), nuevoInicioZonado))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "La nueva franja ya no esta disponible"));

        var inicioValidado=OffsetDateTime.parse(franja.inicio()).toLocalDateTime();
        var finValidado=OffsetDateTime.parse(franja.fin()).toLocalDateTime();
        if (citaRepositorio.buscarConflictosBloqueando(cita.getPrestadorId(),inicioValidado,finValidado,List.of("PENDIENTE","PENDIENTE_PAGO","CONFIRMADA"))
                .stream().anyMatch(otra -> !cita.getId().equals(otra.getId())))
            throw new ResponseStatusException(CONFLICT,"El nuevo horario ya está ocupado");
        String estadoAnterior = cita.getEstado();
        var inicioAnterior = cita.getInicio();
        var finAnterior = cita.getFin();
        cita.setInicio(inicioValidado);
        cita.setFin(OffsetDateTime.parse(franja.fin()).toLocalDateTime());
        cita.setEstado("PENDIENTE");
        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo("PENDIENTE");
        historial.setCambiadoPorUsuarioId(actorId);
        historial.setMotivo(origen == OrigenEventoCita.RECEPCION
                ? "Reprogramacion realizada desde recepción" : "Reprogramacion realizada por el cliente");
        historialEstadoCitaRepositorio.save(historial);

        eventos.publicar(new CitaReprogramada(eventos.datos(cita, origen, actorId), inicioAnterior, finAnterior));

        return mapearCita(cita);
    }

    private boolean coincideInicioFranja(String inicioFranja, OffsetDateTime inicioSolicitado) {
        if (inicioFranja == null || inicioSolicitado == null) {
            return false;
        }
        return OffsetDateTime.parse(inicioFranja).toInstant().equals(inicioSolicitado.toInstant());
    }

    private CitaClienteResponse mapearCita(CitaEntidad cita) {
        return mapearCita(cita, zonasNegocio.de(cita.getEmpresaId(), cita.getSucursalId()));
    }

    private CitaClienteResponse mapearCita(CitaEntidad cita, ZoneId zonaHoraria) {
        String sucursalNombre = sucursalRepositorio.findById(cita.getSucursalId())
                .map(SucursalEntidad::getNombre)
                .orElse("Sucursal");
        String servicioNombre = servicioRepositorio.findById(cita.getServicioId())
                .map(ServicioEntidad::getNombre)
                .orElse("Servicio");
        var prestador = prestadorServicioRepositorio.findById(cita.getPrestadorId()).orElse(null);
        String prestadorNombre = prestador != null ? prestador.getNombreMostrar() : "Prestador";
        String clienteNombre = clienteRepositorio.findById(cita.getClienteId()).map(cliente -> cliente.getNombreCompleto()).orElse("Cliente");
        String clienteTelefono = clienteRepositorio.findById(cita.getClienteId()).map(cliente -> cliente.getTelefono()).orElse("");
        String clienteCorreo = usuarioRepositorio.findById(cita.getClienteId()).map(usuario -> usuario.getCorreo()).orElse("");

        boolean cancelable = List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())
                && cita.getInicio().isAfter(LocalDateTime.now(reloj.withZone(zonaHoraria)));

        return new CitaClienteResponse(
                cita.getId(),
                cita.getEstado(),
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                sucursalNombre,
                servicioNombre,
                prestadorNombre,
                cita.getInicio().atZone(zonaHoraria).toOffsetDateTime(),
                cita.getFin().atZone(zonaHoraria).toOffsetDateTime(),
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
