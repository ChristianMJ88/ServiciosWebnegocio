package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.compartido.correo.ConfirmacionCitaCorreo;
import com.techprotech.agenda.compartido.correo.ServicioCorreoCitas;
import com.techprotech.agenda.compartido.correo.ServicioOutboxCorreoCitas;
import com.techprotech.agenda.compartido.whatsapp.ServicioOutboxWhatsappCitas;
import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolId;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRolRepositorio;
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
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioCitas {

    private static final String PREFIJO_CONTRASENA_PROVISIONAL = "registro-cita";

    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
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
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            ClienteRepositorio clienteRepositorio,
            EmpresaRepositorio empresaRepositorio,
            RolRepositorio rolRepositorio,
            UsuarioRolRepositorio usuarioRolRepositorio,
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
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.empresaRepositorio = empresaRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.usuarioRolRepositorio = usuarioRolRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.servicioCorreoCitas = servicioCorreoCitas;
        this.servicioOutboxCorreoCitas = servicioOutboxCorreoCitas;
        this.servicioOutboxWhatsappCitas = servicioOutboxWhatsappCitas;
    }

    @Transactional
    public CitaCreadaResponse crearCita(CrearCitaRequest request) {
        Long empresaId = request.empresaId() != null ? request.empresaId() : 1L;
        EmpresaEntidad empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa no existe"));

        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaIdAndActivaTrue(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe o no esta activa"));
        ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaIdAndSucursalIdAndActivoTrue(request.servicioId(), empresaId, request.sucursalId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe o no esta activo"));

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
                .filter(franja -> franja.inicio().equals(inicioSolicitado.toString()))
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
        boolean correoConfirmacionProgramado = servicioOutboxCorreoCitas.programarConfirmacion(
                empresaId,
                new ConfirmacionCitaCorreo(
                    cita.getId(),
                    empresa.getNombre(),
                    request.nombreCliente().trim(),
                    request.correoCliente().trim().toLowerCase(),
                    servicio.getNombre(),
                    sucursal.getNombre(),
                    sucursal.getDireccion(),
                    sucursal.getTelefono(),
                    inicioRespuesta,
                    finRespuesta,
                    zona,
                    cita.getPrecio(),
                    cita.getMoneda()
                )
        );
        servicioOutboxWhatsappCitas.programarConfirmacion(
                empresaId,
                cita.getId(),
                request.telefonoCliente().trim(),
                cita.getInicio()
        );

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

    private String resolverMensajeRespuesta(Long empresaId, boolean correoConfirmacionProgramado) {
        if (!servicioCorreoCitas.estaHabilitado(empresaId)) {
            return "Cita creada correctamente";
        }

        if (correoConfirmacionProgramado) {
            return "Cita creada correctamente. Te enviaremos una confirmacion por correo en breve.";
        }

        return "Cita creada correctamente. La empresa no tiene correo de confirmacion disponible en este momento.";
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

    private Long actualizarClienteExistente(Long usuarioId, String nombreCliente, String telefonoCliente) {
        clienteRepositorio.findById(usuarioId).ifPresent(cliente -> {
            String nombreNormalizado = nombreCliente.trim();
            String telefonoNormalizado = telefonoCliente.trim();
            boolean actualizado = false;

            if (!nombreNormalizado.equals(cliente.getNombreCompleto())) {
                cliente.setNombreCompleto(nombreNormalizado);
                actualizado = true;
            }

            if (!telefonoNormalizado.equals(cliente.getTelefono())) {
                cliente.setTelefono(telefonoNormalizado);
                actualizado = true;
            }

            if (actualizado) {
                clienteRepositorio.save(cliente);
            }
        });
        return usuarioId;
    }

    private Long crearCliente(Long empresaId, String nombreCliente, String correo, String telefono) {
        RolEntidad rolCliente = rolRepositorio.findByCodigo("CLIENTE")
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe el rol CLIENTE"));

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

        usuarioRolRepositorio.save(new UsuarioRolEntidad(
                new UsuarioRolId(usuario.getId(), rolCliente.getId(), empresaId),
                usuario,
                rolCliente
        ));

        return usuario.getId();
    }

    private String generarContrasenaProvisional() {
        return PREFIJO_CONTRASENA_PROVISIONAL + "-" + UUID.randomUUID();
    }
}
