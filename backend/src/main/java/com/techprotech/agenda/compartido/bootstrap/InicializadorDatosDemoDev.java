package com.techprotech.agenda.compartido.bootstrap;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.RolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioRolId;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.RolRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRolRepositorio;
import com.techprotech.agenda.modulos.prestadores.infraestructura.entidad.PrestadorServicioEntidad;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class InicializadorDatosDemoDev implements ApplicationRunner {

    private static final String PASSWORD_DEMO = "Temporal123!";

    private final UsuarioRepositorio usuarioRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final PasswordEncoder passwordEncoder;

    public InicializadorDatosDemoDev(
            UsuarioRepositorio usuarioRepositorio,
            RolRepositorio rolRepositorio,
            UsuarioRolRepositorio usuarioRolRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.usuarioRolRepositorio = usuarioRolRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UsuarioEntidad staff = upsertUsuario(1L, "staff.demo@agenda.local");
        UsuarioEntidad admin = upsertUsuario(1L, "admin.demo@agenda.local");

        asignarRol(staff, "STAFF");
        asignarRol(admin, "ADMIN");

        if (prestadorServicioRepositorio.findById(staff.getId()).isEmpty()) {
            PrestadorServicioEntidad prestador = new PrestadorServicioEntidad();
            prestador.setUsuarioId(staff.getId());
            prestador.setSucursalId(1L);
            prestador.setNombreMostrar("Prestador Demo");
            prestador.setBiografia("Prestador inicial de pruebas");
            prestador.setActivo(true);
            prestador.setColorAgenda("#2563eb");
            prestadorServicioRepositorio.save(prestador);
        }
    }

    private UsuarioEntidad upsertUsuario(Long empresaId, String correo) {
        UsuarioEntidad usuario = usuarioRepositorio.findByEmpresaIdAndCorreo(empresaId, correo)
                .orElseGet(UsuarioEntidad::new);
        usuario.setEmpresaId(empresaId);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(passwordEncoder.encode(PASSWORD_DEMO));
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);
        return usuarioRepositorio.save(usuario);
    }

    private void asignarRol(UsuarioEntidad usuario, String codigoRol) {
        RolEntidad rol = rolRepositorio.findByCodigo(codigoRol).orElseThrow();
        UsuarioRolId id = new UsuarioRolId(usuario.getId(), rol.getId(), usuario.getEmpresaId());
        if (usuarioRolRepositorio.findById(id).isEmpty()) {
            usuarioRolRepositorio.save(new UsuarioRolEntidad(id, usuario, rol));
        }
    }
}
