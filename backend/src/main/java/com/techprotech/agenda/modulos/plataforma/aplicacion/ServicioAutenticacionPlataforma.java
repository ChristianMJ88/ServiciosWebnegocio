package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.api.dto.IniciarSesionPlataformaRequest;
import com.techprotech.agenda.modulos.plataforma.api.dto.SesionPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.UsuarioPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.AccesoPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.UsuarioPlataformaRepositorio;
import com.techprotech.agenda.seguridad.jwt.ServicioTokenJwt;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class ServicioAutenticacionPlataforma {
    private final java.time.Clock reloj;
    private final UsuarioPlataformaRepositorio usuarios;
    private final AccesoPlataformaRepositorio acceso;
    private final PasswordEncoder codificador;
    private final ServicioTokenJwt tokens;

    public ServicioAutenticacionPlataforma(UsuarioPlataformaRepositorio usuarios, AccesoPlataformaRepositorio acceso,
                                           PasswordEncoder codificador, ServicioTokenJwt tokens, java.time.Clock reloj) {
        this.reloj = reloj;
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.codificador = codificador;
        this.tokens = tokens;
    }

    @Transactional
    public SesionPlataformaResponse iniciar(IniciarSesionPlataformaRequest request) {
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        UsuarioPlataformaEntidad usuario = usuarios.findByCorreoIgnoreCase(correo)
                .orElseThrow(this::credencialesInvalidas);
        if (!usuario.isHabilitado() || usuario.isBloqueado()
                || !codificador.matches(request.contrasena(), usuario.getContrasenaHash())) {
            throw credencialesInvalidas();
        }
        var roles = acceso.roles(usuario.getId());
        var permisos = acceso.permisos(usuario.getId());
        if (!permisos.contains("PLATAFORMA_ACCESO")) throw credencialesInvalidas();
        usuario.setUltimoAccesoEn(LocalDateTime.now(reloj));
        usuarios.save(usuario);
        String token = tokens.generarTokenPlataforma(correo, usuario.getId(), roles, permisos);
        return new SesionPlataformaResponse(token, "Bearer", usuario.getId(), usuario.getNombre(), correo, roles, permisos);
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }
}
