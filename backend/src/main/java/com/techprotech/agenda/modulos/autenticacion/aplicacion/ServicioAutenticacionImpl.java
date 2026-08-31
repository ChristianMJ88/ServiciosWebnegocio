package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.api.dto.EmpresaAccesoAppResponse;
import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionAppRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RefrescarTokenRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RegistrarClienteRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaAccesoApp;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.TokenActualizacionEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.TokenActualizacionRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.seguridad.jwt.PropiedadesJwt;
import com.techprotech.agenda.seguridad.jwt.ServicioTokenJwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ServicioAutenticacionImpl implements ServicioAutenticacion {

    private static final Logger log = LoggerFactory.getLogger(ServicioAutenticacionImpl.class);
    private static final String CONTRASENA_LEGACY_CITA = "Temporal123!";

    private final ServicioTokenJwt servicioTokenJwt;
    private final PropiedadesJwt propiedadesJwt;
    private final EmpresaRepositorio empresaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ServicioRolesEmpresa servicioRolesEmpresa;
    private final TokenActualizacionRepositorio tokenActualizacionRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final ServicioProteccionAcceso proteccionAcceso;

    public ServicioAutenticacionImpl(
            ServicioTokenJwt servicioTokenJwt,
            PropiedadesJwt propiedadesJwt,
            EmpresaRepositorio empresaRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            ClienteRepositorio clienteRepositorio,
            ServicioRolesEmpresa servicioRolesEmpresa,
            TokenActualizacionRepositorio tokenActualizacionRepositorio,
            PasswordEncoder passwordEncoder,
            ServicioProteccionAcceso proteccionAcceso
    ) {
        this.servicioTokenJwt = servicioTokenJwt;
        this.propiedadesJwt = propiedadesJwt;
        this.empresaRepositorio = empresaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.servicioRolesEmpresa = servicioRolesEmpresa;
        this.tokenActualizacionRepositorio = tokenActualizacionRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.proteccionAcceso = proteccionAcceso;
    }

    @Override
    @Transactional
    public RespuestaTokenJwt iniciarSesion(IniciarSesionRequest request) {
        validarEmpresaExiste(request.empresaId());
        String correoNormalizado = request.correo().trim().toLowerCase();

        UsuarioEntidad usuario = usuarioRepositorio.findByEmpresaIdAndCorreo(request.empresaId(), correoNormalizado)
                .orElseThrow(() -> {
                    log.warn("Intento de inicio de sesion sin usuario: empresaId={}, correo={}", request.empresaId(), correoNormalizado);
                    return new ResponseStatusException(UNAUTHORIZED, "Credenciales invalidas");
                });

        if (!usuario.isHabilitado() || usuario.isBloqueado()) {
            if (esClientePendienteActivacion(usuario)) {
                throw new ResponseStatusException(FORBIDDEN, "Tu acceso aun no esta activado. Completa tu registro para continuar.");
            }
            log.warn(
                    "Intento de inicio de sesion rechazado por estado de usuario: empresaId={}, correo={}, habilitado={}, bloqueado={}",
                    request.empresaId(),
                    correoNormalizado,
                    usuario.isHabilitado(),
                    usuario.isBloqueado()
            );
            throw new ResponseStatusException(FORBIDDEN, "El usuario no tiene acceso habilitado");
        }
        validarBloqueoTemporal(usuario);

        if (esClientePendienteActivacion(usuario) && tieneRolCliente(usuario.getEmpresaId(), usuario.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Tu acceso aun no esta activado. Completa tu registro para continuar.");
        }

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash())) {
            proteccionAcceso.registrarIntentoFallido(usuario.getId());
            log.warn("Intento de inicio de sesion con contrasena invalida: empresaId={}, correo={}", request.empresaId(), correoNormalizado);
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciales invalidas");
        }

        List<String> roles = servicioRolesEmpresa.obtenerCodigosRolUsuario(usuario.getEmpresaId(), usuario.getId());

        usuario.setUltimoAccesoEn(LocalDateTime.now());
        proteccionAcceso.limpiar(usuario);
        usuarioRepositorio.save(usuario);

        return emitirTokens(usuario, roles);
    }

    @Override
    @Transactional
    public RespuestaAccesoApp iniciarSesionApp(IniciarSesionAppRequest request) {
        String correoNormalizado = request.correo().trim().toLowerCase();
        List<UsuarioEntidad> coincidencias = usuarioRepositorio.findByCorreoOrderByEmpresaIdAsc(correoNormalizado);
        if (coincidencias.isEmpty()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciales invalidas");
        }

        List<UsuarioEntidad> candidatos = coincidencias.stream().filter(usuario -> !proteccionAcceso.estaBloqueadoTemporalmente(usuario)).toList();
        if (candidatos.isEmpty()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciales invalidas");
        }
        List<UsuarioEntidad> usuariosConCredenciales = candidatos.stream()
                .filter(usuario -> passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash()))
                .toList();

        if (usuariosConCredenciales.isEmpty()) {
            candidatos.forEach(usuario -> proteccionAcceso.registrarIntentoFallido(usuario.getId()));
            log.warn("Intento de inicio de sesion central con contrasena invalida: correo={}", correoNormalizado);
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciales invalidas");
        }

        List<UsuarioEntidad> usuariosDisponibles = usuariosConCredenciales.stream()
                .filter(this::usuarioPuedeIniciarSesion)
                .toList();

        if (usuariosDisponibles.isEmpty()) {
            throw new ResponseStatusException(FORBIDDEN, "El usuario no tiene acceso habilitado");
        }

        if (request.empresaId() != null) {
            UsuarioEntidad usuarioSeleccionado = usuariosDisponibles.stream()
                    .filter(usuario -> usuario.getEmpresaId().equals(request.empresaId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "No tienes acceso a la empresa seleccionada"));
            return autenticarUsuarioApp(usuarioSeleccionado);
        }

        if (usuariosDisponibles.size() == 1) {
            return autenticarUsuarioApp(usuariosDisponibles.get(0));
        }

        return new RespuestaAccesoApp(
                "SELECCION_EMPRESA",
                "Selecciona la empresa a la que deseas entrar.",
                construirEmpresasAcceso(usuariosDisponibles),
                null
        );
    }

    @Override
    @Transactional
    public RespuestaTokenJwt refrescarToken(RefrescarTokenRequest request) {
        String tokenPlano = request.tokenActualizacion().trim();
        if (!servicioTokenJwt.esValido(tokenPlano)) {
            throw new ResponseStatusException(UNAUTHORIZED, "El token de actualizacion no es valido");
        }

        Long usuarioId = servicioTokenJwt.obtenerUsuarioId(tokenPlano);
        Long empresaId = servicioTokenJwt.obtenerEmpresaId(tokenPlano);
        validarEmpresaExiste(empresaId);

        UsuarioEntidad usuario = usuarioRepositorio.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "La sesion ya no es valida"));

        if (!usuario.isHabilitado() || usuario.isBloqueado()) {
            throw new ResponseStatusException(FORBIDDEN, "El usuario no tiene acceso habilitado");
        }

        TokenActualizacionEntidad tokenGuardado = buscarTokenVigente(usuarioId, tokenPlano)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "El token de actualizacion ya no es valido"));

        tokenGuardado.setRevocadoEn(LocalDateTime.now());
        tokenActualizacionRepositorio.save(tokenGuardado);

        List<String> roles = servicioRolesEmpresa.obtenerCodigosRolUsuario(usuario.getEmpresaId(), usuario.getId());

        return emitirTokens(usuario, roles);
    }

    @Override
    @Transactional
    public void cerrarSesion(RefrescarTokenRequest request) {
        String tokenPlano = request.tokenActualizacion().trim();
        if (!servicioTokenJwt.esValido(tokenPlano)) {
            return;
        }

        Long usuarioId = servicioTokenJwt.obtenerUsuarioId(tokenPlano);
        buscarTokenVigente(usuarioId, tokenPlano).ifPresent(token -> {
            token.setRevocadoEn(LocalDateTime.now());
            tokenActualizacionRepositorio.save(token);
        });
    }

    @Override
    @Transactional
    public void registrarCliente(RegistrarClienteRequest request) {
        validarEmpresaExiste(request.empresaId());

        String correoNormalizado = request.correo().trim().toLowerCase();

        Optional<UsuarioEntidad> usuarioExistente = usuarioRepositorio.findByEmpresaIdAndCorreo(request.empresaId(), correoNormalizado);
        if (usuarioExistente.isPresent()) {
            activarClientePendiente(usuarioExistente.get(), request);
            return;
        }

        UsuarioEntidad usuario = crearNuevoCliente(request, correoNormalizado);
        crearPerfilCliente(usuario.getId(), request.nombreCompleto().trim(), request.telefono().trim());
    }

    private UsuarioEntidad crearNuevoCliente(RegistrarClienteRequest request, String correoNormalizado) {
        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(request.empresaId());
        usuario.setCorreo(correoNormalizado);
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);
        usuario = usuarioRepositorio.save(usuario);
        servicioRolesEmpresa.asignarRolEmpresa(usuario, request.empresaId(), "CLIENTE");
        return usuario;
    }

    private void crearPerfilCliente(Long usuarioId, String nombreCompleto, String telefono) {
        ClienteEntidad cliente = new ClienteEntidad();
        cliente.setUsuarioId(usuarioId);
        cliente.setNombreCompleto(nombreCompleto);
        cliente.setTelefono(telefono);
        cliente.setAceptaWhatsapp(true);
        clienteRepositorio.save(cliente);
    }

    private void activarClientePendiente(UsuarioEntidad usuario, RegistrarClienteRequest request) {
        if (!tieneRolCliente(request.empresaId(), usuario.getId())) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario con ese correo en la empresa");
        }
        if (!esClientePendienteActivacion(usuario)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un usuario con ese correo en la empresa");
        }

        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);
        usuarioRepositorio.save(usuario);

        clienteRepositorio.findById(usuario.getId()).ifPresent(cliente -> {
            cliente.setNombreCompleto(request.nombreCompleto().trim());
            cliente.setTelefono(request.telefono().trim());
            clienteRepositorio.save(cliente);
        });

        servicioRolesEmpresa.asignarRolEmpresa(usuario, request.empresaId(), "CLIENTE");
    }

    private boolean tieneRolCliente(Long empresaId, Long usuarioId) {
        return servicioRolesEmpresa.usuarioTieneRol(empresaId, usuarioId, "CLIENTE");
    }

    private boolean esClientePendienteActivacion(UsuarioEntidad usuario) {
        return !usuario.isHabilitado() || passwordEncoder.matches(CONTRASENA_LEGACY_CITA, usuario.getContrasenaHash());
    }

    private boolean usuarioPuedeIniciarSesion(UsuarioEntidad usuario) {
        if (!usuario.isHabilitado() || usuario.isBloqueado() || proteccionAcceso.estaBloqueadoTemporalmente(usuario)) {
            return false;
        }
        return !(esClientePendienteActivacion(usuario) && tieneRolCliente(usuario.getEmpresaId(), usuario.getId()));
    }

    private void validarEmpresaExiste(Long empresaId) {
        if (!empresaRepositorio.existsById(empresaId)) {
            throw new ResponseStatusException(NOT_FOUND, "La empresa indicada no existe");
        }
    }

    private RespuestaAccesoApp autenticarUsuarioApp(UsuarioEntidad usuario) {
        List<String> roles = servicioRolesEmpresa.obtenerCodigosRolUsuario(usuario.getEmpresaId(), usuario.getId());
        usuario.setUltimoAccesoEn(LocalDateTime.now());
        proteccionAcceso.limpiar(usuario);
        usuarioRepositorio.save(usuario);

        return new RespuestaAccesoApp(
                "AUTENTICADO",
                "Acceso correcto.",
                null,
                emitirTokens(usuario, roles)
        );
    }

    private void validarBloqueoTemporal(UsuarioEntidad usuario) {
        if (proteccionAcceso.estaBloqueadoTemporalmente(usuario)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciales invalidas");
        }
    }

    private RespuestaTokenJwt emitirTokens(UsuarioEntidad usuario, List<String> roles) {
        List<String> permisos = servicioRolesEmpresa.obtenerPermisosUsuario(usuario.getEmpresaId(), usuario.getId());
        List<Long> sucursalesPermitidas = servicioRolesEmpresa.obtenerSucursalesPermitidas(usuario.getEmpresaId(), usuario.getId());
        EmpresaEntidad empresa = empresaRepositorio.findById(usuario.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "La empresa indicada no existe"));
        String tokenAcceso = servicioTokenJwt.generarTokenAcceso(
                usuario.getCorreo(),
                usuario.getId(),
                usuario.getEmpresaId(),
                empresa.getSlug(),
                roles,
                permisos,
                sucursalesPermitidas
        );
        String tokenActualizacion = servicioTokenJwt.generarTokenActualizacion(
                usuario.getCorreo(),
                usuario.getId(),
                usuario.getEmpresaId()
        );

        TokenActualizacionEntidad token = new TokenActualizacionEntidad();
        token.setUsuarioId(usuario.getId());
        token.setTokenHash(passwordEncoder.encode(tokenActualizacion));
        token.setExpiraEn(LocalDateTime.now().plusDays(propiedadesJwt.diasRefresh()));
        tokenActualizacionRepositorio.save(token);

        return new RespuestaTokenJwt(
                tokenAcceso,
                tokenActualizacion,
                "Bearer",
                usuario.getId(),
                usuario.getEmpresaId(),
                empresa.getSlug(),
                empresa.getNombre(),
                roles,
                permisos,
                sucursalesPermitidas
        );
    }

    private List<EmpresaAccesoAppResponse> construirEmpresasAcceso(List<UsuarioEntidad> usuariosDisponibles) {
        Map<Long, EmpresaEntidad> empresasPorId = new LinkedHashMap<>();
        empresaRepositorio.findAllById(
                usuariosDisponibles.stream()
                        .map(UsuarioEntidad::getEmpresaId)
                        .distinct()
                        .toList()
        ).forEach(empresa -> empresasPorId.put(empresa.getId(), empresa));

        return usuariosDisponibles.stream()
                .map(usuario -> {
                    EmpresaEntidad empresa = empresasPorId.get(usuario.getEmpresaId());
                    if (empresa == null) {
                        throw new ResponseStatusException(NOT_FOUND, "La empresa indicada no existe");
                    }
                    List<String> roles = servicioRolesEmpresa.obtenerCodigosRolUsuario(usuario.getEmpresaId(), usuario.getId());
                    List<String> permisos = servicioRolesEmpresa.obtenerPermisosUsuario(usuario.getEmpresaId(), usuario.getId());
                    return new EmpresaAccesoAppResponse(
                            empresa.getId(),
                            empresa.getSlug(),
                            empresa.getNombre(),
                            roles,
                            permisos
                    );
                })
                .sorted(Comparator.comparing(EmpresaAccesoAppResponse::empresaNombre))
                .toList();
    }

    private Optional<TokenActualizacionEntidad> buscarTokenVigente(Long usuarioId, String tokenPlano) {
        return tokenActualizacionRepositorio.findByUsuarioIdAndRevocadoEnIsNullAndExpiraEnAfter(usuarioId, LocalDateTime.now())
                .stream()
                .filter(token -> passwordEncoder.matches(tokenPlano, token.getTokenHash()))
                .findFirst();
    }
}
