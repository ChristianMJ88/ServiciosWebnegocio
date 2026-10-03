package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.UsuarioPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.AccesoPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.UsuarioPlataformaRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class InicializadorAdministradorPlataforma implements ApplicationRunner {
    private final UsuarioPlataformaRepositorio usuarios;
    private final AccesoPlataformaRepositorio acceso;
    private final PasswordEncoder codificador;
    private final boolean habilitado;
    private final String correo;
    private final String nombre;
    private final String contrasena;

    public InicializadorAdministradorPlataforma(UsuarioPlataformaRepositorio usuarios,
            AccesoPlataformaRepositorio acceso, PasswordEncoder codificador,
            @Value("${aplicacion.plataforma.bootstrap.habilitado:false}") boolean habilitado,
            @Value("${aplicacion.plataforma.bootstrap.correo:}") String correo,
            @Value("${aplicacion.plataforma.bootstrap.nombre:Administrador Fluora}") String nombre,
            @Value("${aplicacion.plataforma.bootstrap.contrasena:}") String contrasena) {
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.codificador = codificador;
        this.habilitado = habilitado;
        this.correo = correo;
        this.nombre = nombre;
        this.contrasena = contrasena;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado || correo.isBlank() || contrasena.isBlank() || usuarios.count() > 0) return;
        if (contrasena.length() < 12) {
            throw new IllegalStateException("PLATAFORMA_BOOTSTRAP_CONTRASENA debe tener al menos 12 caracteres");
        }
        UsuarioPlataformaEntidad usuario = new UsuarioPlataformaEntidad();
        usuario.setCorreo(correo.trim().toLowerCase(Locale.ROOT));
        usuario.setNombre(nombre.trim());
        usuario.setContrasenaHash(codificador.encode(contrasena));
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);
        usuario = usuarios.save(usuario);
        acceso.asignarRol(usuario.getId(), "ADMINISTRADOR_PLATAFORMA");
    }
}
