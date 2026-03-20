package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.api.dto.CitaCreadaResponse;
import com.techprotech.agenda.modulos.citas.api.dto.CrearCitaRequest;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolId;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServicioCitas {

    private final ServicioConsultaDisponibilidad servicioConsultaDisponibilidad;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final PasswordEncoder passwordEncoder;

    public ServicioCitas(
            ServicioConsultaDisponibilidad servicioConsultaDisponibilidad,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            ClienteRepositorio clienteRepositorio,
            RolRepositorio rolRepositorio,
            UsuarioRolRepositorio usuarioRolRepositorio,
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            PasswordEncoder passwordEncoder
    ) {
        this.servicioConsultaDisponibilidad = servicioConsultaDisponibilidad;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.usuarioRolRepositorio = usuarioRolRepositorio;
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CitaCreadaResponse crearCita(CrearCitaRequest request) {
        Long empresaId = request.empresaId() != null ? request.empresaId() : 1L;

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

        return new CitaCreadaResponse(
                cita.getId(),
                cita.getEstado(),
                cita.getEmpresaId(),
                cita.getSucursalId(),
                cita.getServicioId(),
                cita.getPrestadorId(),
                cita.getInicio().atZone(zona).toOffsetDateTime(),
                cita.getFin().atZone(zona).toOffsetDateTime(),
                "Cita creada correctamente"
        );
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
                .map(UsuarioEntidad::getId)
                .orElseGet(() -> crearCliente(empresaId, nombreCliente, correo, telefonoCliente));
    }

    private Long crearCliente(Long empresaId, String nombreCliente, String correo, String telefono) {
        RolEntidad rolCliente = rolRepositorio.findByCodigo("CLIENTE")
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe el rol CLIENTE"));

        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(empresaId);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(passwordEncoder.encode("Temporal123!"));
        usuario.setHabilitado(true);
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
}
