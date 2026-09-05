package com.techprotech.agenda.modulos.autenticacion.api;

import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionAppRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.IniciarSesionRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.PerfilUsuarioResponse;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RefrescarTokenRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RegistrarClienteRequest;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaAccesoApp;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioAutenticacion;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioInternoPerfilRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.seguridad.cookies.ServicioCookieTokenActualizacion;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/auth")
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAutenticacion;
    private final UsuarioInternoPerfilRepositorio usuarioInternoPerfilRepositorio;
    private final ServicioCookieTokenActualizacion cookies;

    public ControladorAutenticacion(
            ServicioAutenticacion servicioAutenticacion,
            UsuarioInternoPerfilRepositorio usuarioInternoPerfilRepositorio,
            ServicioCookieTokenActualizacion cookies
    ) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.usuarioInternoPerfilRepositorio = usuarioInternoPerfilRepositorio;
        this.cookies = cookies;
    }

    @GetMapping("/perfil")
    public ResponseEntity<PerfilUsuarioResponse> perfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (usuario == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "La sesión no es válida");
        }
        return usuarioInternoPerfilRepositorio.findById(usuario.usuarioId())
                .map(perfil -> ResponseEntity.ok(new PerfilUsuarioResponse(
                        usuario.usuarioId(),
                        usuario.correo(),
                        perfil.getNombreCompleto(),
                        perfil.getPuesto(),
                        perfil.getSucursalId()
                )))
                .orElseGet(() -> ResponseEntity.ok(new PerfilUsuarioResponse(
                        usuario.usuarioId(),
                        usuario.correo(),
                        null,
                        null,
                        null
                )));
    }

    @PostMapping("/iniciar-sesion")
    public ResponseEntity<RespuestaTokenJwt> iniciarSesion(
            @Valid @RequestBody IniciarSesionRequest request,
            HttpServletResponse response
    ) {
        RespuestaTokenJwt sesion = servicioAutenticacion.iniciarSesion(request);
        cookies.agregar(response, sesion);
        return ResponseEntity.ok(sesion);
    }

    @PostMapping("/app-login")
    public ResponseEntity<RespuestaAccesoApp> iniciarSesionApp(
            @Valid @RequestBody IniciarSesionAppRequest request,
            HttpServletResponse response
    ) {
        RespuestaAccesoApp acceso = servicioAutenticacion.iniciarSesionApp(request);
        cookies.agregar(response, acceso);
        return ResponseEntity.ok(acceso);
    }

    @PostMapping("/registrar-cliente")
    public ResponseEntity<Void> registrarCliente(@Valid @RequestBody RegistrarClienteRequest request) {
        servicioAutenticacion.registrarCliente(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refrescar-token")
    public ResponseEntity<RespuestaTokenJwt> refrescarToken(
            @CookieValue(name = ServicioCookieTokenActualizacion.NOMBRE_COOKIE, required = false) String tokenCookie,
            HttpServletResponse response
    ) {
        RespuestaTokenJwt sesion = servicioAutenticacion.refrescarToken(cookies.resolver(tokenCookie));
        cookies.agregar(response, sesion);
        return ResponseEntity.ok(sesion);
    }

    @PostMapping("/cerrar-sesion")
    public ResponseEntity<Void> cerrarSesion(
            @CookieValue(name = ServicioCookieTokenActualizacion.NOMBRE_COOKIE, required = false) String tokenCookie,
            HttpServletResponse response
    ) {
        try {
            servicioAutenticacion.cerrarSesion(cookies.resolver(tokenCookie));
        } catch (ResponseStatusException ignored) {
            // Cerrar sesión es idempotente incluso si la cookie ya expiró.
        }
        cookies.eliminar(response);
        return ResponseEntity.noContent().build();
    }
}
