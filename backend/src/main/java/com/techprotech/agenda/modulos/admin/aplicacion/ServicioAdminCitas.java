package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.compartido.correo.ConfiguracionCorreoEmpresaEntidad;
import com.techprotech.agenda.compartido.correo.ConfiguracionCorreoEmpresaRepositorio;
import com.techprotech.agenda.compartido.correo.ProveedorCorreo;
import com.techprotech.agenda.compartido.correo.ProtectorSecretosCorreo;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ConfiguracionCorreoAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.MigracionSecretosCorreoResponse;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.PrestadorAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ReporteServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ServicioAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.ServicioAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.SucursalAdminRequest;
import com.techprotech.agenda.modulos.admin.api.dto.SucursalAdminResponse;
import com.techprotech.agenda.modulos.admin.api.dto.ResumenAdminResponse;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolId;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRolRepositorio;
import com.techprotech.agenda.modulos.citas.api.dto.CitaClienteResponse;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.HistorialEstadoCitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.HistorialEstadoCitaRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.AsignacionServicioPrestadorId;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.AsignacionServicioPrestadorRepositorio;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ServicioAdminCitas {

    private final CitaRepositorio citaRepositorio;
    private final HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio;
    private final SucursalRepositorio sucursalRepositorio;
    private final ServicioRepositorio servicioRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final RolRepositorio rolRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracionCorreoEmpresaRepositorio configuracionCorreoEmpresaRepositorio;
    private final ProtectorSecretosCorreo protectorSecretosCorreo;

    public ServicioAdminCitas(
            CitaRepositorio citaRepositorio,
            HistorialEstadoCitaRepositorio historialEstadoCitaRepositorio,
            SucursalRepositorio sucursalRepositorio,
            ServicioRepositorio servicioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            AsignacionServicioPrestadorRepositorio asignacionServicioPrestadorRepositorio,
            ClienteRepositorio clienteRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            UsuarioRolRepositorio usuarioRolRepositorio,
            RolRepositorio rolRepositorio,
            PasswordEncoder passwordEncoder,
            ConfiguracionCorreoEmpresaRepositorio configuracionCorreoEmpresaRepositorio,
            ProtectorSecretosCorreo protectorSecretosCorreo
    ) {
        this.citaRepositorio = citaRepositorio;
        this.historialEstadoCitaRepositorio = historialEstadoCitaRepositorio;
        this.sucursalRepositorio = sucursalRepositorio;
        this.servicioRepositorio = servicioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.asignacionServicioPrestadorRepositorio = asignacionServicioPrestadorRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.usuarioRolRepositorio = usuarioRolRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.configuracionCorreoEmpresaRepositorio = configuracionCorreoEmpresaRepositorio;
        this.protectorSecretosCorreo = protectorSecretosCorreo;
    }

    @Transactional(readOnly = true)
    public ResumenAdminResponse resumen(Long empresaId) {
        List<CitaEntidad> citas = citaRepositorio.findByEmpresaIdOrderByInicioDesc(empresaId);
        LocalDate hoy = LocalDate.now();
        long total = citas.size();
        return new ResumenAdminResponse(
                total,
                contarEstado(citas, "PENDIENTE"),
                contarEstado(citas, "CONFIRMADA"),
                contarEstado(citas, "FINALIZADA"),
                contarEstado(citas, "CANCELADA"),
                contarEstado(citas, "NO_ASISTIO"),
                citas.stream().filter(cita -> cita.getInicio().toLocalDate().isEqual(hoy)).count(),
                sumarIngresos(citas, Set.of("PENDIENTE", "CONFIRMADA", "FINALIZADA")),
                sumarIngresos(citas, Set.of("FINALIZADA"))
        );
    }

    @Transactional(readOnly = true)
    public List<ReporteServicioAdminResponse> reporteServicios(Long empresaId) {
        Map<Long, ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(ServicioEntidad::getId, servicio -> servicio));
        return citaRepositorio.findByEmpresaIdOrderByInicioDesc(empresaId).stream()
                .collect(Collectors.groupingBy(CitaEntidad::getServicioId))
                .entrySet()
                .stream()
                .map(entry -> {
                    ServicioEntidad servicio = servicios.get(entry.getKey());
                    List<CitaEntidad> citasServicio = entry.getValue();
                    return new ReporteServicioAdminResponse(
                            entry.getKey(),
                            servicio != null ? servicio.getNombre() : "Servicio",
                            citasServicio.size(),
                            contarEstado(citasServicio, "PENDIENTE"),
                            contarEstado(citasServicio, "CONFIRMADA"),
                            contarEstado(citasServicio, "FINALIZADA"),
                            sumarIngresos(citasServicio, Set.of("PENDIENTE", "CONFIRMADA", "FINALIZADA")),
                            sumarIngresos(citasServicio, Set.of("FINALIZADA"))
                    );
                })
                .sorted(Comparator.comparing(ReporteServicioAdminResponse::totalCitas).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfiguracionCorreoAdminResponse obtenerConfiguracionCorreo(Long empresaId) {
        ConfiguracionCorreoEmpresaEntidad configuracion = configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionCorreoEmpresaEntidad nueva = new ConfiguracionCorreoEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    nueva.setHabilitado(false);
                    return nueva;
                });

        return mapearConfiguracionCorreo(configuracion);
    }

    @Transactional
    public ConfiguracionCorreoAdminResponse actualizarConfiguracionCorreo(Long empresaId, ConfiguracionCorreoAdminRequest request) {
        ConfiguracionCorreoEmpresaEntidad configuracion = configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .orElseGet(() -> {
                    ConfiguracionCorreoEmpresaEntidad nueva = new ConfiguracionCorreoEmpresaEntidad();
                    nueva.setEmpresaId(empresaId);
                    return nueva;
        });

        ProveedorCorreo proveedor = ProveedorCorreo.desdeValor(request.proveedor(), ProveedorCorreo.SMTP);
        configuracion.setHabilitado(request.habilitado());
        configuracion.setProveedor(proveedor.name());
        configuracion.setRemitente(normalizarOpcional(request.remitente()));
        configuracion.setNombreRemitente(normalizarOpcional(request.nombreRemitente()));
        configuracion.setResponderA(normalizarOpcional(request.responderA()));
        configuracion.setSmtpHost(normalizarOpcional(request.smtpHost()));
        configuracion.setSmtpPort(request.smtpPort());
        configuracion.setSmtpUsername(normalizarOpcional(request.smtpUsername()));
        configuracion.setSmtpAuth(request.smtpAuth());
        configuracion.setSmtpStartTls(request.smtpStartTls());
        configuracion.setGraphTenantId(normalizarOpcional(request.graphTenantId()));
        configuracion.setGraphClientId(normalizarOpcional(request.graphClientId()));
        configuracion.setGraphUserId(normalizarOpcional(request.graphUserId()));
        configuracion.setGraphCertificateThumbprint(normalizarOpcional(request.graphCertificateThumbprint()));

        if (request.smtpPassword() != null) {
            String passwordNormalizado = normalizarOpcional(request.smtpPassword());
            configuracion.setSmtpPassword(
                    passwordNormalizado != null ? protectorSecretosCorreo.encriptar(passwordNormalizado) : null
            );
        }

        if (request.graphClientSecret() != null) {
            String secretoNormalizado = normalizarOpcional(request.graphClientSecret());
            configuracion.setGraphClientSecret(
                    secretoNormalizado != null ? protectorSecretosCorreo.encriptar(secretoNormalizado) : null
            );
        }

        if (request.graphPrivateKeyPem() != null) {
            String llavePrivadaNormalizada = normalizarOpcional(request.graphPrivateKeyPem());
            configuracion.setGraphPrivateKeyPem(
                    llavePrivadaNormalizada != null ? protectorSecretosCorreo.encriptar(llavePrivadaNormalizada) : null
            );
        }

        return mapearConfiguracionCorreo(configuracionCorreoEmpresaRepositorio.save(configuracion));
    }

    @Transactional
    public MigracionSecretosCorreoResponse migrarSecretosCorreo(Long empresaId) {
        ConfiguracionCorreoEmpresaEntidad configuracion = configuracionCorreoEmpresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe configuracion de correo para la empresa"));

        boolean actualizada = false;
        StringBuilder mensaje = new StringBuilder();

        String smtpPassword = configuracion.getSmtpPassword();
        if (smtpPassword != null && !smtpPassword.isBlank() && !smtpPassword.startsWith("enc:v1:")) {
            configuracion.setSmtpPassword(protectorSecretosCorreo.encriptar(smtpPassword));
            actualizada = true;
            mensaje.append("smtp_password migrado");
        }

        String graphClientSecret = configuracion.getGraphClientSecret();
        if (graphClientSecret != null && !graphClientSecret.isBlank() && !graphClientSecret.startsWith("enc:v1:")) {
            configuracion.setGraphClientSecret(protectorSecretosCorreo.encriptar(graphClientSecret));
            if (mensaje.length() > 0) {
                mensaje.append("; ");
            }
            mensaje.append("graph_client_secret migrado");
            actualizada = true;
        }

        String graphPrivateKeyPem = configuracion.getGraphPrivateKeyPem();
        if (graphPrivateKeyPem != null && !graphPrivateKeyPem.isBlank() && !graphPrivateKeyPem.startsWith("enc:v1:")) {
            configuracion.setGraphPrivateKeyPem(protectorSecretosCorreo.encriptar(graphPrivateKeyPem));
            if (mensaje.length() > 0) {
                mensaje.append("; ");
            }
            mensaje.append("graph_private_key_pem migrado");
            actualizada = true;
        }

        if (!actualizada) {
            return new MigracionSecretosCorreoResponse(false, "No hay secretos legacy por migrar");
        }

        configuracionCorreoEmpresaRepositorio.save(configuracion);
        return new MigracionSecretosCorreoResponse(true, mensaje + " a formato cifrado");
    }

    @Transactional(readOnly = true)
    public List<CitaClienteResponse> listar(Long empresaId) {
        return citaRepositorio.findByEmpresaIdOrderByInicioDesc(empresaId)
                .stream()
                .map(cita -> new CitaClienteResponse(
                        cita.getId(),
                        cita.getEstado(),
                        cita.getSucursalId(),
                        cita.getServicioId(),
                        cita.getPrestadorId(),
                        sucursalRepositorio.findById(cita.getSucursalId()).map(s -> s.getNombre()).orElse("Sucursal"),
                        servicioRepositorio.findById(cita.getServicioId()).map(s -> s.getNombre()).orElse("Servicio"),
                        prestadorServicioRepositorio.findById(cita.getPrestadorId()).map(p -> p.getNombreMostrar()).orElse("Prestador"),
                        cita.getInicio().atZone(ZoneId.of(sucursalRepositorio.findById(cita.getSucursalId()).map(s -> s.getZonaHoraria()).orElse("America/Mexico_City"))).toOffsetDateTime(),
                        cita.getFin().atZone(ZoneId.of(sucursalRepositorio.findById(cita.getSucursalId()).map(s -> s.getZonaHoraria()).orElse("America/Mexico_City"))).toOffsetDateTime(),
                        cita.getPrecio(),
                        cita.getMoneda(),
                        cita.getNotas(),
                        false,
                        clienteRepositorio.findById(cita.getClienteId()).map(cliente -> cliente.getNombreCompleto()).orElse("Cliente"),
                        usuarioRepositorio.findById(cita.getClienteId()).map(usuario -> usuario.getCorreo()).orElse(""),
                        clienteRepositorio.findById(cita.getClienteId()).map(cliente -> cliente.getTelefono()).orElse("")
                ))
                .toList();
    }

    @Transactional
    public void cambiarEstadoCita(Long empresaId, Long usuarioAdminId, Long citaId, String nuevoEstado) {
        CitaEntidad cita = citaRepositorio.findByIdAndEmpresaId(citaId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La cita no existe para la empresa"));

        if (!List.of("CONFIRMADA", "FINALIZADA", "NO_ASISTIO", "CANCELADA").contains(nuevoEstado)) {
            throw new ResponseStatusException(BAD_REQUEST, "El estado solicitado no es valido para administracion");
        }

        String estadoAnterior = cita.getEstado();
        cita.setEstado(nuevoEstado);

        if ("CANCELADA".equals(nuevoEstado)) {
            cita.setCanceladaEn(java.time.LocalDateTime.now());
            cita.setMotivoCancelacion("Cancelada desde el panel administrativo");
        }

        citaRepositorio.save(cita);

        HistorialEstadoCitaEntidad historial = new HistorialEstadoCitaEntidad();
        historial.setCitaId(cita.getId());
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setCambiadoPorUsuarioId(usuarioAdminId);
        historial.setMotivo("Cambio de estado realizado por administracion");
        historialEstadoCitaRepositorio.save(historial);
    }

    @Transactional(readOnly = true)
    public List<SucursalAdminResponse> listarSucursales(Long empresaId) {
        return sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId)
                .stream()
                .map(this::mapearSucursal)
                .toList();
    }

    @Transactional
    public SucursalAdminResponse crearSucursal(Long empresaId, SucursalAdminRequest request) {
        SucursalEntidad sucursal = new SucursalEntidad();
        sucursal.setEmpresaId(empresaId);
        aplicarSucursal(sucursal, request);
        return mapearSucursal(sucursalRepositorio.save(sucursal));
    }

    @Transactional
    public SucursalAdminResponse actualizarSucursal(Long empresaId, Long sucursalId, SucursalAdminRequest request) {
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(sucursalId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal no existe para la empresa"));
        aplicarSucursal(sucursal, request);
        return mapearSucursal(sucursalRepositorio.save(sucursal));
    }

    @Transactional(readOnly = true)
    public List<ServicioAdminResponse> listarServicios(Long empresaId) {
        Map<Long, String> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(java.util.stream.Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        return servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId)
                .stream()
                .map(servicio -> mapearServicio(servicio, sucursales.getOrDefault(servicio.getSucursalId(), "Sucursal")))
                .toList();
    }

    @Transactional
    public ServicioAdminResponse crearServicio(Long empresaId, ServicioAdminRequest request) {
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));
        ServicioEntidad servicio = new ServicioEntidad();
        servicio.setEmpresaId(empresaId);
        aplicarServicio(servicio, request);
        return mapearServicio(servicioRepositorio.save(servicio), sucursal.getNombre());
    }

    @Transactional
    public ServicioAdminResponse actualizarServicio(Long empresaId, Long servicioId, ServicioAdminRequest request) {
        ServicioEntidad servicio = servicioRepositorio.findByIdAndEmpresaId(servicioId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El servicio no existe para la empresa"));
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));
        aplicarServicio(servicio, request);
        return mapearServicio(servicioRepositorio.save(servicio), sucursal.getNombre());
    }

    @Transactional(readOnly = true)
    public List<PrestadorAdminResponse> listarPrestadores(Long empresaId) {
        List<SucursalEntidad> sucursales = sucursalRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId);
        if (sucursales.isEmpty()) {
            return List.of();
        }

        Map<Long, String> sucursalNombres = sucursales.stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, SucursalEntidad::getNombre));
        List<Long> sucursalIds = sucursales.stream().map(SucursalEntidad::getId).toList();
        List<PrestadorServicioEntidad> prestadores = prestadorServicioRepositorio.findBySucursalIdInOrderByNombreMostrarAsc(sucursalIds);
        if (prestadores.isEmpty()) {
            return List.of();
        }

        List<Long> prestadorIds = prestadores.stream().map(PrestadorServicioEntidad::getUsuarioId).toList();
        Map<Long, UsuarioEntidad> usuarios = usuarioRepositorio.findByEmpresaIdAndIdIn(empresaId, prestadorIds).stream()
                .collect(Collectors.toMap(UsuarioEntidad::getId, usuario -> usuario));
        Map<Long, ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(ServicioEntidad::getId, servicio -> servicio));
        Map<Long, List<AsignacionServicioPrestadorEntidad>> asignacionesPorPrestador =
                asignacionServicioPrestadorRepositorio.findByIdPrestadorIdIn(prestadorIds).stream()
                        .filter(AsignacionServicioPrestadorEntidad::isActiva)
                        .collect(Collectors.groupingBy(asignacion -> asignacion.getId().getPrestadorId()));

        return prestadores.stream()
                .map(prestador -> mapearPrestador(
                        prestador,
                        usuarios.get(prestador.getUsuarioId()),
                        sucursalNombres.getOrDefault(prestador.getSucursalId(), "Sucursal"),
                        asignacionesPorPrestador.getOrDefault(prestador.getUsuarioId(), List.of()),
                        servicios
                ))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public PrestadorAdminResponse crearPrestador(Long empresaId, PrestadorAdminRequest request) {
        if (request.contrasenaTemporal() == null || request.contrasenaTemporal().isBlank()) {
            throw new ResponseStatusException(CONFLICT, "Debes indicar una contrasena temporal para el prestador");
        }

        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));

        usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, request.correo().trim().toLowerCase())
                .ifPresent(usuario -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe un usuario con ese correo en la empresa");
                });

        Map<Long, ServicioEntidad> serviciosSeleccionados = validarServiciosAsignados(empresaId, request.sucursalId(), request.servicioIds());

        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(empresaId);
        usuario.setCorreo(request.correo().trim().toLowerCase());
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasenaTemporal()));
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);
        usuario = usuarioRepositorio.save(usuario);

        asignarRolStaff(usuario, empresaId);

        PrestadorServicioEntidad prestador = new PrestadorServicioEntidad();
        prestador.setUsuarioId(usuario.getId());
        aplicarPrestador(prestador, request);
        prestador = prestadorServicioRepositorio.save(prestador);

        sincronizarAsignaciones(prestador.getUsuarioId(), request.servicioIds());
        return mapearPrestador(prestador, usuario, sucursal.getNombre(), asignacionServicioPrestadorRepositorio.findByIdPrestadorId(prestador.getUsuarioId()), serviciosSeleccionados);
    }

    @Transactional
    public PrestadorAdminResponse actualizarPrestador(Long empresaId, Long prestadorId, PrestadorAdminRequest request) {
        UsuarioEntidad usuario = usuarioRepositorio.findByIdAndEmpresaId(prestadorId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El usuario del prestador no existe para la empresa"));
        PrestadorServicioEntidad prestador = prestadorServicioRepositorio.findById(prestadorId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El prestador no existe"));
        SucursalEntidad sucursal = sucursalRepositorio.findByIdAndEmpresaId(request.sucursalId(), empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La sucursal indicada no existe para la empresa"));

        String correoNormalizado = request.correo().trim().toLowerCase();
        usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, correoNormalizado)
                .filter(existente -> !existente.getId().equals(prestadorId))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(CONFLICT, "Ya existe otro usuario con ese correo en la empresa");
                });

        Map<Long, ServicioEntidad> serviciosSeleccionados = validarServiciosAsignados(empresaId, request.sucursalId(), request.servicioIds());

        usuario.setCorreo(correoNormalizado);
        if (request.contrasenaTemporal() != null && !request.contrasenaTemporal().isBlank()) {
            usuario.setContrasenaHash(passwordEncoder.encode(request.contrasenaTemporal()));
        }
        usuario.setHabilitado(request.activo());
        usuario.setBloqueado(false);
        usuarioRepositorio.save(usuario);

        aplicarPrestador(prestador, request);
        prestador = prestadorServicioRepositorio.save(prestador);

        sincronizarAsignaciones(prestadorId, request.servicioIds());
        return mapearPrestador(prestador, usuario, sucursal.getNombre(), asignacionServicioPrestadorRepositorio.findByIdPrestadorId(prestadorId), serviciosSeleccionados);
    }

    private void aplicarSucursal(SucursalEntidad sucursal, SucursalAdminRequest request) {
        sucursal.setNombre(request.nombre().trim());
        sucursal.setDireccion(request.direccion() != null ? request.direccion().trim() : null);
        sucursal.setTelefono(request.telefono() != null ? request.telefono().trim() : null);
        sucursal.setZonaHoraria(request.zonaHoraria().trim());
        sucursal.setActiva(request.activa());
    }

    private ConfiguracionCorreoAdminResponse mapearConfiguracionCorreo(ConfiguracionCorreoEmpresaEntidad configuracion) {
        String smtpPassword = configuracion.getSmtpPassword();
        boolean smtpConfigurada = smtpPassword != null && !smtpPassword.isBlank();
        boolean smtpCifrada = smtpConfigurada && smtpPassword.startsWith("enc:v1:");
        String graphClientSecret = configuracion.getGraphClientSecret();
        boolean graphConfigurada = graphClientSecret != null && !graphClientSecret.isBlank();
        boolean graphCifrada = graphConfigurada && graphClientSecret.startsWith("enc:v1:");
        String graphPrivateKeyPem = configuracion.getGraphPrivateKeyPem();
        boolean graphPrivateKeyConfigurada = graphPrivateKeyPem != null && !graphPrivateKeyPem.isBlank();
        boolean graphPrivateKeyCifrada = graphPrivateKeyConfigurada && graphPrivateKeyPem.startsWith("enc:v1:");

        return new ConfiguracionCorreoAdminResponse(
                configuracion.isHabilitado(),
                configuracion.getProveedor(),
                configuracion.getRemitente(),
                configuracion.getNombreRemitente(),
                configuracion.getResponderA(),
                configuracion.getSmtpHost(),
                configuracion.getSmtpPort(),
                configuracion.getSmtpUsername(),
                smtpConfigurada,
                smtpCifrada,
                (smtpConfigurada && !smtpCifrada) || (graphConfigurada && !graphCifrada) || (graphPrivateKeyConfigurada && !graphPrivateKeyCifrada),
                configuracion.getSmtpAuth(),
                configuracion.getSmtpStartTls(),
                configuracion.getGraphTenantId(),
                configuracion.getGraphClientId(),
                configuracion.getGraphUserId(),
                graphConfigurada,
                graphCifrada,
                configuracion.getGraphCertificateThumbprint(),
                graphPrivateKeyConfigurada,
                graphPrivateKeyCifrada
        );
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private SucursalAdminResponse mapearSucursal(SucursalEntidad sucursal) {
        return new SucursalAdminResponse(
                sucursal.getId(),
                sucursal.getNombre(),
                sucursal.getDireccion(),
                sucursal.getTelefono(),
                sucursal.getZonaHoraria(),
                sucursal.isActiva()
        );
    }

    private void aplicarServicio(ServicioEntidad servicio, ServicioAdminRequest request) {
        servicio.setSucursalId(request.sucursalId());
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setBufferAntesMinutos(request.bufferAntesMinutos());
        servicio.setBufferDespuesMinutos(request.bufferDespuesMinutos());
        servicio.setPrecio(request.precio());
        servicio.setMoneda(request.moneda().trim().toUpperCase());
        servicio.setActivo(request.activo());
    }

    private ServicioAdminResponse mapearServicio(ServicioEntidad servicio, String sucursalNombre) {
        return new ServicioAdminResponse(
                servicio.getId(),
                servicio.getSucursalId(),
                sucursalNombre,
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getDuracionMinutos(),
                servicio.getBufferAntesMinutos(),
                servicio.getBufferDespuesMinutos(),
                servicio.getPrecio(),
                servicio.getMoneda(),
                servicio.isActivo()
        );
    }

    private void aplicarPrestador(PrestadorServicioEntidad prestador, PrestadorAdminRequest request) {
        prestador.setSucursalId(request.sucursalId());
        prestador.setNombreMostrar(request.nombreMostrar().trim());
        prestador.setBiografia(request.biografia() != null ? request.biografia().trim() : null);
        prestador.setColorAgenda(request.colorAgenda() != null && !request.colorAgenda().isBlank() ? request.colorAgenda().trim() : "#2563eb");
        prestador.setActivo(request.activo());
    }

    private void asignarRolStaff(UsuarioEntidad usuario, Long empresaId) {
        RolEntidad rolStaff = rolRepositorio.findByCodigo("STAFF")
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe el rol STAFF"));
        UsuarioRolId usuarioRolId = new UsuarioRolId(usuario.getId(), rolStaff.getId(), empresaId);
        if (!usuarioRolRepositorio.existsById(usuarioRolId)) {
            usuarioRolRepositorio.save(new UsuarioRolEntidad(usuarioRolId, usuario, rolStaff));
        }
    }

    private Map<Long, ServicioEntidad> validarServiciosAsignados(Long empresaId, Long sucursalId, List<Long> servicioIds) {
        List<Long> idsSolicitados = servicioIds != null ? servicioIds.stream().distinct().toList() : List.of();
        if (idsSolicitados.isEmpty()) {
            return Map.of();
        }

        List<ServicioEntidad> servicios = servicioRepositorio.findByEmpresaIdAndIdIn(empresaId, idsSolicitados);
        if (servicios.size() != idsSolicitados.size()) {
            throw new ResponseStatusException(NOT_FOUND, "Uno o mas servicios no existen para la empresa");
        }

        boolean todosEnSucursal = servicios.stream().allMatch(servicio -> servicio.getSucursalId().equals(sucursalId));
        if (!todosEnSucursal) {
            throw new ResponseStatusException(CONFLICT, "Todos los servicios asignados deben pertenecer a la misma sucursal del prestador");
        }

        return servicios.stream().collect(Collectors.toMap(ServicioEntidad::getId, servicio -> servicio));
    }

    private void sincronizarAsignaciones(Long prestadorId, List<Long> servicioIds) {
        Set<Long> seleccionados = servicioIds != null ? new HashSet<>(servicioIds) : Collections.emptySet();
        List<AsignacionServicioPrestadorEntidad> actuales = asignacionServicioPrestadorRepositorio.findByIdPrestadorId(prestadorId);
        Map<Long, AsignacionServicioPrestadorEntidad> actualesPorServicio = actuales.stream()
                .collect(Collectors.toMap(asignacion -> asignacion.getId().getServicioId(), asignacion -> asignacion));

        for (AsignacionServicioPrestadorEntidad asignacion : actuales) {
            asignacion.setActiva(seleccionados.contains(asignacion.getId().getServicioId()));
        }

        for (Long servicioId : seleccionados) {
            if (!actualesPorServicio.containsKey(servicioId)) {
                AsignacionServicioPrestadorEntidad nueva = new AsignacionServicioPrestadorEntidad();
                nueva.setId(new AsignacionServicioPrestadorId(prestadorId, servicioId));
                nueva.setActiva(true);
                asignacionServicioPrestadorRepositorio.save(nueva);
            }
        }

        if (!actuales.isEmpty()) {
            asignacionServicioPrestadorRepositorio.saveAll(actuales);
        }
    }

    private PrestadorAdminResponse mapearPrestador(
            PrestadorServicioEntidad prestador,
            UsuarioEntidad usuario,
            String sucursalNombre,
            Collection<AsignacionServicioPrestadorEntidad> asignaciones,
            Map<Long, ServicioEntidad> servicios
    ) {
        if (usuario == null) {
            return null;
        }

        List<ServicioEntidad> serviciosAsignados = asignaciones.stream()
                .filter(AsignacionServicioPrestadorEntidad::isActiva)
                .map(asignacion -> servicios.get(asignacion.getId().getServicioId()))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(ServicioEntidad::getNombre))
                .toList();

        return new PrestadorAdminResponse(
                prestador.getUsuarioId(),
                prestador.getSucursalId(),
                sucursalNombre,
                usuario.getCorreo(),
                prestador.getNombreMostrar(),
                prestador.getBiografia(),
                prestador.getColorAgenda(),
                prestador.isActivo(),
                serviciosAsignados.stream().map(ServicioEntidad::getId).toList(),
                serviciosAsignados.stream().map(ServicioEntidad::getNombre).toList()
        );
    }

    private long contarEstado(List<CitaEntidad> citas, String estado) {
        return citas.stream().filter(cita -> estado.equals(cita.getEstado())).count();
    }

    private BigDecimal sumarIngresos(List<CitaEntidad> citas, Set<String> estados) {
        return citas.stream()
                .filter(cita -> estados.contains(cita.getEstado()))
                .map(CitaEntidad::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
