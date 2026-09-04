package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.correo.ServicioCorreoCitas;
import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CitasMultiplesCreadasResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaMultipleItemRequest;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitasMultiplesRequest;
import com.techprotech.agenda.modulos.clientes.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.clientes.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.FranjaDisponibleResponse;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConsultaDisponibilidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.AsignacionServicioPrestadorRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioSucursalRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioCitas {

    private static final String PREFIJO_CONTRASENA_PROVISIONAL = "registro-cita";
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioCitas.class);

    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final ServicioSucursalRepositorio servicioSucursalRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final ServicioRolesEmpresa servicioRolesEmpresa;
    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final ServicioCorreoCitas servicioCorreoCitas;
    private final ServicioOutboxCorreoCitas servicioOutboxCorreoCitas;
    private final ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas;

    public ServicioCitas(
            ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            ServicioSucursalRepositorio servicioSucursalRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            ClienteRepositorio clienteRepositorio,
            EmpresaRepositorio empresaRepositorio,
            ServicioRolesEmpresa servicioRolesEmpresa,
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            PasswordEncoder passwordEncoder,
            ServicioCorreoCitas servicioCorreoCitas,
            ServicioOutboxCorreoCitas servicioOutboxCorreoCitas,
            ServicioOutboxWhatsappCitas servicioOutboxWhatsappCitas
    ) {
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.servicioSucursalRepositorio = servicioSucursalRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.empresaRepositorio = empresaRepositorio;
        this.servicioRolesEmpresa = servicioRolesEmpresa;
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.servicioCorreoCitas = servicioCorreoCitas;
        this.servicioOutboxCorreoCitas = servicioOutboxCorreoCitas;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
    }

    @Transactional
    public CitaCreadaResponse crearCita(CrearCitaRequest request) {
        return crearCita(request, true);
    }

    private CitaCreadaResponse crearCita(CrearCitaRequest request, boolean programarWhatsappConfirmacion) {
        Long empresaId = request.empresaId() != null ? request.empresaId() : 1L;
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa no existe"));

        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe o no esta activa"));
        ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaIdAndActivoTrue(request.servicioId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo"));
        if (!servicioSucursalRepositorio.existsByEmpresaIdAndIdServicioIdAndIdSucursalIdAndActivoTrue(empresaId, request.servicioId(), request.sucursalId())) {
            throw new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo en la sucursal");
        }

        PrestadorServicioEntidad prestador = resolverPrestador(request.prestadorId(), request.sucursalId(), request.servicioId());
        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());
        OffsetDateTime inicioSolicitado = request.inicio().atZoneSameInstant(zona).toOffsetDateTime();
        LocalDate fechaLocal = inicioSolicitado.toLocalDate();

        List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                empresaId,
                request.sucursalId(),
                request.servicioId(),
                prestador.getUsuarioId(),
                fechaLocal
        );

        FranjaDisponibleResponse franjaSeleccionada = franjas.stream()
                .filter(franja -> coincideInicioFranja(franja.inicio(), inicioSolicitado))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "La franja solicitada ya no esta disponible"));

        LocalDateTime inicio = OffsetDateTime.parse(franjaSeleccionada.inicio()).toLocalDateTime();
        LocalDateTime fin = OffsetDateTime.parse(franjaSeleccionada.fin()).toLocalDateTime();

        if (!citaRepositorio.buscarConflictos(prestador.getUsuarioId(), inicio, fin, List.of("PENDIENTE", "CONFIRMADA")).isEmpty()) {
            throw new ResponseStatusException(CONFLICT, "Existe un conflicto de horario para el prestador");
        }

        Long clienteId = resolverOCrearCliente(
                empresaId,
                request.nombreCliente(),
                request.correoCliente(),
                request.telefonoCliente()
        );

        CitaEntidad cita = new CitaEntidad();
        cita.setEmpresaId(empresaId);
        cita.setSucursalId(request.sucursalId());
        cita.setServicioId(request.servicioId());
        cita.setPrestadorId(prestador.getUsuarioId());
        cita.setClienteId(clienteId);
        cita.setEstado("PENDIENTE");
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setMoneda(servicio.getMoneda());
        cita.setNotas(request.notas());
        cita.setCreadaPorUsuarioId(clienteId);
        cita = citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo("PENDIENTE");
        historial.setCambiadoPorUsuarioId(clienteId);
        historial.setMotivo("Creacion inicial de cita");
        historialEstadoCitaRepositorio.save(historial);

        OffsetDateTime inicioRespuesta = cita.getInicio().atZone(zona).toOffsetDateTime();
        OffsetDateTime finRespuesta = cita.getFin().atZone(zona).toOffsetDateTime();
        boolean correoConfirmacionProgramado = servicioOutboxCorreoCitas.programarRegistrada(
                empresaId, cita.getId(), cita.getInicio());
        if (programarWhatsappConfirmacion) {
            servicioOutboxWhatsappCitas.programarConfirmacion(
                    empresaId,
                    cita.getId(),
                    request.telefonoCliente().trim(),
                    cita.getInicio()
            );
        }

        return new CitaCreadaResponse(
                cita.getId(),
                cita.getEstado(),
                cita.getEmpresaId(),
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                inicioRespuesta,
                finRespuesta,
                resolverMensajeRespuesta(empresaId, correoConfirmacionProgramado),
                correoConfirmacionProgramado,
                false
        );
    }

    @Transactional
    public CitasMultiplesCreadasResponse crearCitasMultiples(CrearCitasMultiplesRequest request) {
        Long empresaId = request.empresaId() != null ? request.empresaId() : 1L;
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa no existe"));
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe o no esta activa"));
        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());

        List<CrearCitaMultipleItemRequest> items = request.items().stream()
                .sorted(Comparator.comparing(CrearCitaMultipleItemRequest::inicio))
                .toList();

        if (items.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debes indicar al menos un servicio para reservar");
        }

        List<RangoReservaValidado> rangos = new ArrayList<>();
        for (CrearCitaMultipleItemRequest item : items) {
            List<FranjaDisponibleResponse> franjas = servicioConsultaDisponibilidad.obtenerFranjasDisponibles(
                    request.empresaId(),
                    request.sucursalId(),
                    item.servicioId(),
                    item.prestadorId(),
                    item.inicio().toLocalDate()
            );

            FranjaDisponibleResponse franja = franjas.stream()
                    .filter(disponible -> coincideInicioFranja(disponible.inicio(), item.inicio()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(CONFLICT, "Uno de los horarios seleccionados ya no esta disponible"));

            rangos.add(new RangoReservaValidado(
                    item.servicioId(),
                    franja.prestadorId(),
                    OffsetDateTime.parse(franja.inicio()),
                    OffsetDateTime.parse(franja.fin())
            ));
        }

        for (int index = 1; index < rangos.size(); index++) {
            RangoReservaValidado anterior = rangos.get(index - 1);
            RangoReservaValidado actual = rangos.get(index);
            OffsetDateTime finAnterior = normalizarMinuto(anterior.fin());
            OffsetDateTime inicioActual = normalizarMinuto(actual.inicio());
            if (inicioActual.isBefore(finAnterior)) {
                LOGGER.warn(
                        "Conflicto al crear citas multiples empresa={} sucursal={} servicioAnterior={} finAnterior={} servicioActual={} inicioActual={}",
                        request.empresaId(),
                        request.sucursalId(),
                        anterior.servicioId(),
                        anterior.fin(),
                        actual.servicioId(),
                        actual.inicio()
                );
                throw new ResponseStatusException(CONFLICT, "Los servicios seleccionados se traslapan. Ajusta el itinerario antes de confirmar.");
            }
        }

        Long clienteId = resolverOCrearCliente(
                empresaId,
                request.nombreCliente(),
                request.correoCliente(),
                request.telefonoCliente()
        );

        List<CitaCreadaResponse> citas = new ArrayList<>();
        for (RangoReservaValidado rango : rangos) {
            ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaIdAndActivoTrue(rango.servicioId(), empresaId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo"));

            CitaEntidad cita = new CitaEntidad();
            cita.setEmpresaId(empresaId);
            cita.setSucursalId(request.sucursalId());
            cita.setServicioId(rango.servicioId());
            cita.setPrestadorId(rango.prestadorId());
            cita.setClienteId(clienteId);
            cita.setEstado("PENDIENTE");
            cita.setInicio(rango.inicio().toLocalDateTime());
            cita.setFin(rango.fin().toLocalDateTime());
            cita.setPrecio(servicio.getPrecio());
            cita.setMoneda(servicio.getMoneda());
            cita.setNotas(request.notas());
            cita.setCreadaPorUsuarioId(clienteId);
            cita = citaRepositorio.save(cita);

            HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
            historial.setCitaId(cita.getId());
            historial.setEstadoAnterior(null);
            historial.setEstadoNuevo("PENDIENTE");
            historial.setCambiadoPorUsuarioId(clienteId);
            historial.setMotivo("Creacion inicial de cita");
            historialEstadoCitaRepositorio.save(historial);

            OffsetDateTime inicioRespuesta = cita.getInicio().atZone(zona).toOffsetDateTime();
            OffsetDateTime finRespuesta = cita.getFin().atZone(zona).toOffsetDateTime();
            boolean correoConfirmacionProgramado = servicioOutboxCorreoCitas.programarRegistrada(
                    empresaId, cita.getId(), cita.getInicio());

            citas.add(new CitaCreadaResponse(
                    cita.getId(),
                    cita.getEstado(),
                    cita.getEmpresaId(),
                    cita.getSucursalId(),
                    cita.getServicioId(),
                    cita.getPrestadorId(),
                    inicioRespuesta,
                    finRespuesta,
                    resolverMensajeRespuesta(empresaId, correoConfirmacionProgramado),
                    correoConfirmacionProgramado,
                    false
            ));
        }

        if (!citas.isEmpty()) {
            servicioOutboxWhatsappCitas.programarConfirmacion(
                    empresaId,
                    citas.getFirst().id(),
                    request.telefonoCliente().trim(),
                    citas.getFirst().inicio().toLocalDateTime(),
                    citas.size()
            );
        }

        return new CitasMultiplesCreadasResponse(
                citas,
                citas.size(),
                "Las reservas fueron creadas correctamente sin choques de horario."
        );
    }

    private String resolverMensajeRespuesta(Long empresaId, boolean correoConfirmacionProgramado) {
        if (!servicioCorreoCitas.estaHabilitado(empresaId)) {
            return "Cita creada correctamente";
        }

        if (correoConfirmacionProgramado) {
            return "Cita creada correctamente. Te enviaremos una confirmacion por correo en breve.";
        }

        return "Cita creada correctamente. La empresa no tiene correo de confirmacion disponible en este momento.";
    }

    private OffsetDateTime normalizarMinuto(OffsetDateTime fechaHora) {
        return fechaHora == null ? null : fechaHora.truncatedTo(ChronoUnit.MINUTES);
    }

    private boolean coincideInicioFranja(String inicioFranja, OffsetDateTime inicioSolicitado) {
        if (inicioFranja == null || inicioSolicitado == null) {
            return false;
        }
        return OffsetDateTime.parse(inicioFranja).toInstant().equals(inicioSolicitado.toInstant());
    }

    private PrestadorServicioEntidad resolverPrestador(Long prestadorId, Long sucursalId, Long servicioId) {
        if (prestadorId != null) {
            PrestadorServicioEntidad prestador = prestadorServicioRepositorio.findByUsuarioIdAndSucursalIdAndActivoTrue(prestadorId, sucursalId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El prestador no existe o no pertenece a la sucursal"));

            asignacionServicioPrestadorRepositorio.findByIdPrestadorIdAndIdServicioIdAndActivaTrue(prestadorId, servicioId)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El prestador no ofrece el servicio indicado"));
            return prestador;
        }

        AsignacionServicioPrestadorEntidad asignacion = asignacionServicioPrestadorRepositorio.findByIdServicioIdAndActivaTrue(servicioId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No hay prestadores activos para el servicio"));

        return prestadorServicioRepositorio.findByUsuarioIdAndSucursalIdAndActivoTrue(asignacion.getId().getPrestadorId(), sucursalId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No hay prestadores activos en la sucursal"));
    }

    private Long resolverOCrearCliente(Long empresaId, String nombreCliente, String correoCliente, String telefonoCliente) {
        String correo = correoCliente.trim().toLowerCase();
        return usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, correo)
                .map(usuario -> actualizarClienteExistente(usuario.getId(), nombreCliente, telefonoCliente))
                .orElseGet(() -> crearCliente(empresaId, nombreCliente, correo, telefonoCliente));
    }

    Long actualizarClienteExistente(Long usuarioId, String nombreCliente, String telefonoCliente) {
        String nombreNormalizado = nombreCliente.trim();
        String telefonoNormalizado = telefonoCliente.trim();
        ClienteEntidad cliente = clienteRepositorio.findById(usuarioId).orElseGet(() -> {
            ClienteEntidad nuevoCliente = new ClienteEntidad();
            nuevoCliente.setUsuarioId(usuarioId);
            nuevoCliente.setAceptaWhatsapp(true);
            return nuevoCliente;
        });

        boolean nuevoPerfil = cliente.getNombreCompleto() == null;
        boolean nombreCambio = !nombreNormalizado.equals(cliente.getNombreCompleto());
        boolean telefonoCambio = !telefonoNormalizado.equals(cliente.getTelefono());
        if (nuevoPerfil || nombreCambio || telefonoCambio) {
            cliente.setNombreCompleto(nombreNormalizado);
            cliente.setTelefono(telefonoNormalizado);
            clienteRepositorio.save(cliente);
        }
        return usuarioId;
    }

    private Long crearCliente(Long empresaId, String nombreCliente, String correo, String telefono) {
        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(empresaId);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(passwordEncoder.encode(generarContrasenaProvisional()));
        usuario.setHabilitado(false);
        usuario.setBloqueado(false);
        usuario = usuarioRepositorio.save(usuario);

        ClienteEntidad cliente = new ClienteEntidad();
        cliente.setUsuarioId(usuario.getId());
        cliente.setNombreCompleto(nombreCliente.trim());
        cliente.setTelefono(telefono.trim());
        cliente.setAceptaWhatsapp(true);
        clienteRepositorio.save(cliente);

        servicioRolesEmpresa.asignarRolEmpresa(usuario, empresaId, "CLIENTE");

        return usuario.getId();
    }

    private String generarContrasenaProvisional() {
        return PREFIJO_CONTRASENA_PROVISIONAL + "-" + UUID.randomUUID();
    }

    private record RangoReservaValidado(
            Long servicioId,
            Long prestadorId,
            OffsetDateTime inicio,
            OffsetDateTime fin
    ) {
    }
}
