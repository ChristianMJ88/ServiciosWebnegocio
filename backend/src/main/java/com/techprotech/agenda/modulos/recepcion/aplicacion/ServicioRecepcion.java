package com.techprotech.agenda.modulos.recepcion.aplicacion;

import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitas;
import com.techprotech.agenda.modulos.citas.aplicacion.ServicioCitasCliente;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.recepcion.api.dto.CitaRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.ClienteRecepcionResponse;
import com.techprotech.agenda.modulos.recepcion.api.dto.CrearCitaRecepcionRequest;
import com.techprotech.agenda.modulos.recepcion.api.dto.ReagendarRecepcionRequest;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioRecepcion {

    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final ServicioCitas servicioCitas;
    private final ServicioCitasCliente servicioCitasCliente;
    private final ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas;

    public ServicioRecepcion(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            ServicioCitas servicioCitas,
            ServicioCitasCliente servicioCitasCliente,
            ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas
    ) {
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.servicioCitas = servicioCitas;
        this.servicioCitasCliente = servicioCitasCliente;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
    }

    @Transactional(readOnly = true)
    public List<CitaRecepcionResponse> agenda(Long empresaId, LocalDate fecha, Long sucursalId) {
        LocalDate fechaOperativa = fecha != null ? fecha : LocalDate.now();
        List<CitaEntidad> citas = citaRepositorio.findByEmpresaIdAndInicioBetweenOrderByInicioAsc(
                empresaId,
                fechaOperativa.atStartOfDay(),
                fechaOperativa.plusDays(1).atStartOfDay()
        );

        return citas.stream()
                .filter(cita -> sucursalId == null || sucursalId.equals(cita.getSucursalId()))
                .map(this::mapearCita)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteRecepcionResponse> buscarClientes(Long empresaId, String texto) {
        String consulta = texto == null ? "" : texto.trim();
        if (consulta.length() < 2) {
            return List.of();
        }

        List<ClienteEntidad> clientes = clienteRepositorio.buscarPorNombreOTelefono(consulta);
        Map<Long, UsuarioEntidad> usuarios = usuarioRepositorio.findByEmpresaIdAndIdIn(
                        empresaId,
                        clientes.stream().map(ClienteEntidad::getUsuarioId).toList()
                ).stream()
                .collect(Collectors.toMap(UsuarioEntidad::getId, Function.identity()));

        return clientes.stream()
                .filter(cliente -> usuarios.containsKey(cliente.getUsuarioId()))
                .limit(12)
                .map(cliente -> new ClienteRecepcionResponse(
                        cliente.getUsuarioId(),
                        cliente.getNombreCompleto(),
                        cliente.getTelefono(),
                        usuarios.get(cliente.getUsuarioId()).getCorreo(),
                        cliente.isAceptaWhatsapp()
                ))
                .toList();
    }

    @Transactional
    public CitaCreadaResponse crearCita(Long empresaId, CrearCitaRecepcionRequest request) {
        return servicioCitas.crearCita(new CrearCitaRequest(
                empresaId,
                request.sucursalId(),
                request.servicioId(),
                request.prestadorId(),
                request.nombreCliente(),
                request.correoCliente(),
                request.telefonoCliente(),
                request.inicio(),
                request.notas()
        ));
    }

    @Transactional
    public CitaRecepcionResponse checkIn(Long empresaId, Long usuarioId, Long citaId) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, citaId);
        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se puede hacer check-in a citas pendientes o confirmadas");
        }

        cita.setCheckInEn(LocalDateTime.now());
        cita.setCheckInPorUsuarioId(usuarioId);
        if ("PENDIENTE".equals(cita.getEstado())) {
            String estadoAnterior = cita.getEstado();
            cita.setEstado("CONFIRMADA");
            guardarHistorial(cita.getId(), estadoAnterior, "CONFIRMADA", usuarioId, "Check-in en recepción");
        }
        return mapearCita(citaRepositorio.save(cita));
    }

    @Transactional
    public CitaRecepcionResponse confirmar(Long empresaId, Long usuarioId, Long citaId) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, citaId);
        if (!"PENDIENTE".equals(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden confirmar citas pendientes");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado("CONFIRMADA");
        citaRepositorio.save(cita);
        guardarHistorial(cita.getId(), estadoAnterior, "CONFIRMADA", usuarioId, "Confirmación desde recepción");
        return mapearCita(cita);
    }

    @Transactional
    public CitaRecepcionResponse finalizar(Long empresaId, Long usuarioId, Long citaId) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, citaId);
        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden finalizar citas pendientes o confirmadas");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado("FINALIZADA");
        citaRepositorio.save(cita);
        guardarHistorial(cita.getId(), estadoAnterior, "FINALIZADA", usuarioId, "Cierre desde recepción");
        clienteRepositorio.findById(cita.getClienteId())
                .filter(cliente -> cliente.isAceptaWhatsapp() && cliente.getTelefono() != null && !cliente.getTelefono().isBlank())
                .ifPresent(cliente -> servicioOutboxWhatsappCitas.programarGraciasVisita(
                        empresaId,
                        cita.getId(),
                        cliente.getTelefono(),
                        cita.getInicio()
                ));
        return mapearCita(cita);
    }

    @Transactional
    public CitaRecepcionResponse cancelar(Long empresaId, Long usuarioId, Long citaId) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, citaId);
        if (!List.of("PENDIENTE", "CONFIRMADA").contains(cita.getEstado())) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden cancelar citas pendientes o confirmadas");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado("CANCELADA");
        cita.setCanceladaEn(LocalDateTime.now());
        cita.setMotivoCancelacion("Cancelada desde recepción");
        citaRepositorio.save(cita);
        guardarHistorial(cita.getId(), estadoAnterior, "CANCELADA", usuarioId, "Cancelación desde recepción");
        clienteRepositorio.findById(cita.getClienteId())
                .filter(cliente -> cliente.isAceptaWhatsapp() && cliente.getTelefono() != null && !cliente.getTelefono().isBlank())
                .ifPresent(cliente -> servicioOutboxWhatsappCitas.programarCancelacionNegocio(
                        empresaId,
                        cita.getId(),
                        cliente.getTelefono(),
                        cita.getInicio()
                ));
        return mapearCita(cita);
    }

    @Transactional
    public CitaClienteResponse reprogramar(Long empresaId, Long usuarioId, Long citaId, ReagendarRecepcionRequest request) {
        CitaEntidad cita = obtenerCitaEmpresa(empresaId, citaId);
        return servicioCitasCliente.reprogramar(empresaId, cita.getClienteId(), citaId, request.nuevoInicio());
    }

    private CitaEntidad obtenerCitaEmpresa(Long empresaId, Long citaId) {
        return citaRepositorio.findByIdAndEmpresaId(citaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));
    }

    private void guardarHistorial(Long citaId, String estadoAnterior, String estadoNuevo, Long usuarioId, String motivo) {
        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(citaId);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setCambiadoPorUsuarioId(usuarioId);
        historial.setMotivo(motivo);
        historialEstadoCitaRepositorio.save(historial);
    }

    private CitaRecepcionResponse mapearCita(CitaEntidad cita) {
        SucursalEntidad sucursal = sucursalRepositorio.findById(cita.getSucursalId()).orElse(null);
        String zonaHoraria = sucursal != null ? sucursal.getZonaHoraria() : "America/Mexico_City";
        String sucursalNombre = sucursal != null ? sucursal.getNombre() : "Sucursal";
        String servicioNombre = servicioRepositorio.findById(cita.getServicioId()).map(servicio -> servicio.getNombre()).orElse("Servicio");
        String prestadorNombre = prestadorServicioRepositorio.findById(cita.getPrestadorId()).map(prestador -> prestador.getNombreMostrar()).orElse("Prestador");
        String clienteNombre = clienteRepositorio.findById(cita.getClienteId()).map(ClienteEntidad::getNombreCompleto).orElse("Cliente");
        String clienteTelefono = clienteRepositorio.findById(cita.getClienteId()).map(ClienteEntidad::getTelefono).orElse("");
        String clienteCorreo = usuarioRepositorio.findById(cita.getClienteId()).map(UsuarioEntidad::getCorreo).orElse("");
        ZoneId zoneId = ZoneId.of(zonaHoraria);

        return new CitaRecepcionResponse(
                cita.getId(),
                cita.getEstado(),
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                sucursalNombre,
                servicioNombre,
                prestadorNombre,
                clienteNombre,
                clienteCorreo,
                clienteTelefono,
                cita.getInicio().atZone(zoneId).toOffsetDateTime(),
                cita.getFin().atZone(zoneId).toOffsetDateTime(),
                cita.getPrecio(),
                cita.getMoneda(),
                cita.getNotas(),
                cita.getCheckInEn(),
                cita.getCheckInPorUsuarioId()
        );
    }
}
