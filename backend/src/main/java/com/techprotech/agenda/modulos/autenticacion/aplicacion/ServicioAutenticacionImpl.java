package com.techprotech.agenda.modulos.autenticacion.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RefrescarTokenRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RegistrarClienteRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.ClienteEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.TokenActualizacionEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolId;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.ClienteRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.TokenActualizacionRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRolRepositorio;
import com.techprotech.agenda.seguridad.jwt.PropiedadesJwt;
import com.techprotech.agenda.seguridad.jwt.ServicioTokenJwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
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
    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final TokenActualizacionRepositorio tokenActualizacionRepositorio;
    private final PasswordEncoder passwordEncoder;

    public ServicioAutenticacionImpl(
            ServicioTokenJwt servicioTokenJwt,
            PropiedadesJwt propiedadesJwt,
            EmpresaRepositorio empresaRepositorio,
            UsuarioRepositorio usuarioRepositorio,
            ClienteRepositorio clienteRepositorio,
            RolRepositorio rolRepositorio,
            UsuarioRolRepositorio usuarioRolRepositorio,
            TokenActualizacionRepositorio tokenActualizacionRepositorio,
            PasswordEncoder passwordEncoder
    ) {
        this.servicioTokenJwt = servicioTokenJwt;
        this.propiedadesJwt = propiedadesJwt;
        this.empresaRepositorio = empresaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.usuarioRolRepositorio = usuarioRolRepositorio;
        this.tokenActualizacionRepositorio = tokenActualizacionRepositorio;
        this.passwordEncoder = passwordEncoder;
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

        if (esClientePendienteActivacion(usuario) && tieneRolCliente(usuario.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Tu acceso aun no esta activado. Completa tu registro para continuar.");
        }

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash())) {
            log.warn("Intento de inicio de sesion con contrasena invalida: empresaId={}, correo={}", request.empresaId(), correoNormalizado);
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciales invalidas");
        }

        List<String> roles = usuarioRolRepositorio.findByUsuario_Id(usuario.getId())
                .stream()
                .map(usuarioRol -> usuarioRol.getRol().getCodigo())
                .toList();

        usuario.setUltimoAccesoEn(LocalDateTime.now());
        usuarioRepositorio.save(usuario);

        return emitirTokens(usuario, roles);
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

        List<String> roles = usuarioRolRepositorio.findByUsuario_Id(usuario.getId())
                .stream()
                .map(usuarioRol -> usuarioRol.getRol().getCodigo())
                .toList();

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

        RolEntidad rolCliente = rolRepositorio.findByCodigo("CLIENTE")
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe el rol CLIENTE"));

        Optional<UsuarioEntidad> usuarioExistente = usuarioRepositorio.findByEmpresaIdAndCorreo(request.empresaId(), correoNormalizado);
        if (usuarioExistente.isPresent()) {
            activarClientePendiente(usuarioExistente.get(), rolCliente, request);
            return;
        }

        UsuarioEntidad usuario = crearNuevoCliente(rolCliente, request, correoNormalizado);
        crearPerfilCliente(usuario.getId(), request.nombreCompleto().trim(), request.telefono().trim());
    }

    private UsuarioEntidad crearNuevoCliente(RolEntidad rolCliente, RegistrarClienteRequest request, String correoNormalizado) {
        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setEmpresaId(request.empresaId());
        usuario.setCorreo(correoNormalizado);
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);
        usuario = usuarioRepositorio.save(usuario);

        UsuarioRolEntidad usuarioRol = new UsuarioRolEntidad(
                new UsuarioRolId(usuario.getId(), rolCliente.getId(), request.empresaId()),
                usuario,
                rolCliente
        );
        usuarioRolRepositorio.save(usuarioRol);
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

    private void activarClientePendiente(UsuarioEntidad usuario, RolEntidad rolCliente, RegistrarClienteRequest request) {
        if (!tieneRolCliente(usuario.getId())) {
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

        if (usuarioRolRepositorio.findByUsuario_Id(usuario.getId()).isEmpty()) {
            usuarioRolRepositorio.save(new UsuarioRolEntidad(
                    new UsuarioRolId(usuario.getId(), rolCliente.getId(), request.empresaId()),
                    usuario,
                    rolCliente
            ));
        }
    }

    private boolean tieneRolCliente(Long usuarioId) {
        return usuarioRolRepositorio.findByUsuario_Id(usuarioId)
                .stream()
                .anyMatch(usuarioRol -> "CLIENTE".equals(usuarioRol.getRol().getCodigo()));
    }

    private boolean esClientePendienteActivacion(UsuarioEntidad usuario) {
        return !usuario.isHabilitado() || passwordEncoder.matches(CONTRASENA_LEGACY_CITA, usuario.getContrasenaHash());
    }

    private void validarEmpresaExiste(Long empresaId) {
        if (!empresaRepositorio.existsById(empresaId)) {
            throw new ResponseStatusException(NOT_FOUND, "La empresa indicada no existe");
        }
    }

    private RespuestaTokenJwt emitirTokens(UsuarioEntidad usuario, List<String> roles) {
        String tokenAcceso = servicioTokenJwt.generarTokenAcceso(
                usuario.getCorreo(),
                usuario.getId(),
                usuario.getEmpresaId(),
                roles
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
                roles
        );
    }

    private Optional<TokenActualizacionEntidad> buscarTokenVigente(Long usuarioId, String tokenPlano) {
        return tokenActualizacionRepositorio.findByUsuarioIdAndRevocadoEnIsNullAndExpiraEnAfter(usuarioId, LocalDateTime.now())
                .stream()
                .filter(token -> passwordEncoder.matches(tokenPlano, token.getTokenHash()))
                .findFirst();
    }
}
