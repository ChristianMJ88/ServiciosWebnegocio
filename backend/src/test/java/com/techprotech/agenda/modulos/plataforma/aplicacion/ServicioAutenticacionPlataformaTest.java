package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.api.dto.IniciarSesionPlataformaRequest;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.UsuarioPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.AccesoPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.UsuarioPlataformaRepositorio;
import com.techprotech.agenda.seguridad.jwt.ServicioTokenJwt;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ServicioAutenticacionPlataformaTest {
    @Test
    void emiteUnaSesionConRolesYPermisosExclusivosDePlataforma() {
        UsuarioPlataformaRepositorio usuarios = mock(UsuarioPlataformaRepositorio.class);
        AccesoPlataformaRepositorio acceso = mock(AccesoPlataformaRepositorio.class);
        PasswordEncoder codificador = mock(PasswordEncoder.class);
        ServicioTokenJwt tokens = mock(ServicioTokenJwt.class);
        UsuarioPlataformaEntidad usuario = usuario(3L, true, false);
        when(usuarios.findByCorreoIgnoreCase("admin@refluora.com")).thenReturn(Optional.of(usuario));
        when(codificador.matches("secreto-seguro", "hash")).thenReturn(true);
        when(acceso.roles(3L)).thenReturn(List.of("ADMINISTRADOR_PLATAFORMA"));
        when(acceso.permisos(3L)).thenReturn(List.of("PLATAFORMA_ACCESO", "PLATAFORMA_TENANTS_VER"));
        when(tokens.generarTokenPlataforma(anyString(), eq(3L), anyList(), anyList())).thenReturn("jwt-plataforma");
        ServicioAutenticacionPlataforma servicio = new ServicioAutenticacionPlataforma(usuarios, acceso, codificador, tokens, java.time.Clock.systemUTC());

        var respuesta = servicio.iniciar(new IniciarSesionPlataformaRequest(" ADMIN@refluora.com ", "secreto-seguro"));

        assertThat(respuesta.tokenAcceso()).isEqualTo("jwt-plataforma");
        assertThat(respuesta.roles()).containsExactly("ADMINISTRADOR_PLATAFORMA");
        assertThat(respuesta.permisos()).contains("PLATAFORMA_TENANTS_VER");
        verify(usuarios).save(usuario);
    }

    @Test
    void rechazaUsuariosSinPermisoDeAccesoAunqueLaContrasenaSeaValida() {
        UsuarioPlataformaRepositorio usuarios = mock(UsuarioPlataformaRepositorio.class);
        AccesoPlataformaRepositorio acceso = mock(AccesoPlataformaRepositorio.class);
        PasswordEncoder codificador = mock(PasswordEncoder.class);
        UsuarioPlataformaEntidad usuario = usuario(4L, true, false);
        when(usuarios.findByCorreoIgnoreCase("operador@refluora.com")).thenReturn(Optional.of(usuario));
        when(codificador.matches(anyString(), anyString())).thenReturn(true);
        when(acceso.roles(4L)).thenReturn(List.of("ROL_SIN_ACCESO"));
        when(acceso.permisos(4L)).thenReturn(List.of("PLATAFORMA_TENANTS_VER"));
        ServicioAutenticacionPlataforma servicio = new ServicioAutenticacionPlataforma(
                usuarios, acceso, codificador, mock(ServicioTokenJwt.class), java.time.Clock.systemUTC());

        assertThatThrownBy(() -> servicio.iniciar(
                new IniciarSesionPlataformaRequest("operador@refluora.com", "secreto-seguro")))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    private UsuarioPlataformaEntidad usuario(Long id, boolean habilitado, boolean bloqueado) {
        UsuarioPlataformaEntidad usuario = new UsuarioPlataformaEntidad();
        usuario.setId(id);
        usuario.setCorreo(id == 3L ? "admin@refluora.com" : "operador@refluora.com");
        usuario.setNombre("Equipo Fluora");
        usuario.setContrasenaHash("hash");
        usuario.setHabilitado(habilitado);
        usuario.setBloqueado(bloqueado);
        return usuario;
    }
}
