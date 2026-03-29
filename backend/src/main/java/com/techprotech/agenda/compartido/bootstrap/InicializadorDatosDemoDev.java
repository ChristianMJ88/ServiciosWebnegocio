package com.techprotech.agenda.compartido.bootstrap;

import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
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
    private final PrestadorServicioRepositorio prestadorServicioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final ServicioRolesEmpresa servicioRolesEmpresa;

    public InicializadorDatosDemoDev(
            UsuarioRepositorio usuarioRepositorio,
            PrestadorServicioRepositorio prestadorServicioRepositorio,
            PasswordEncoder passwordEncoder,
            ServicioRolesEmpresa servicioRolesEmpresa
    ) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.prestadorServicioRepositorio = prestadorServicioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.servicioRolesEmpresa = servicioRolesEmpresa;
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
        servicioRolesEmpresa.asignarRolEmpresa(usuario, usuario.getEmpresaId(), codigoRol);
    }
}
