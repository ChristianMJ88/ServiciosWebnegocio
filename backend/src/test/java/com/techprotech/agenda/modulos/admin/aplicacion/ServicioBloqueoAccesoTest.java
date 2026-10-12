package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.modulos.admin.api.dto.BloqueoAccesoRequest;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaRolEmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.prestadores.infraestructura.repositorio.PrestadorServicioRepositorio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioBloqueoAccesoTest {
    final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    final UsuarioInternoPerfilRepositorio internos = mock(UsuarioInternoPerfilRepositorio.class);
    final PrestadorServicioRepositorio prestadores = mock(PrestadorServicioRepositorio.class);
    final AuditoriaRolEmpresaRepositorio auditoria = mock(AuditoriaRolEmpresaRepositorio.class);
    final ServicioBloqueoAcceso servicio = new ServicioBloqueoAcceso(usuarios, internos, prestadores, auditoria);
    final UsuarioAutenticado actor = new UsuarioAutenticado("administrador",1L,10L,"TENANT",List.of(),List.of(),List.of());

    UsuarioEntidad preparar(boolean bloqueado) {
        var usuario = new UsuarioEntidad(); usuario.setId(2L); usuario.setEmpresaId(10L);
        usuario.setHabilitado(false); usuario.setBloqueado(bloqueado);
        when(usuarios.bloquearPorIdYEmpresa(2L,10L)).thenReturn(Optional.of(usuario));
        when(internos.existsById(2L)).thenReturn(true);
        return usuario;
    }
    @Test void bloqueoRevocaSesionYRegistraMotivo() {
        var usuario = preparar(false);
        var resultado = servicio.cambiar(actor,2L,false,new BloqueoAccesoRequest(true,"Suspensión solicitada"));
        assertTrue(resultado.bloqueado()); assertEquals(1L,usuario.getVersionSesion()); verify(auditoria).save(any());
    }
    @Test void desbloqueoNoReactivaNiRestauraTokens() {
        var usuario = preparar(true);
        var resultado = servicio.cambiar(actor,2L,false,new BloqueoAccesoRequest(false,"Revisión terminada"));
        assertFalse(resultado.bloqueado()); assertFalse(resultado.habilitado()); assertEquals(1L,usuario.getVersionSesion());
    }
    @Test void otroTenantNoPuedeCambiarUsuario() {
        assertThrows(ResponseStatusException.class,()->servicio.cambiar(actor,2L,false,new BloqueoAccesoRequest(true,"Motivo")));
        verifyNoInteractions(internos,prestadores,auditoria); verify(usuarios,never()).save(any());
    }
    @Test void noPermiteBloquearseASiMismo() {
        var usuario = new UsuarioEntidad();when(usuarios.bloquearPorIdYEmpresa(1L,10L)).thenReturn(Optional.of(usuario));
        assertThrows(ResponseStatusException.class,()->servicio.cambiar(actor,1L,false,new BloqueoAccesoRequest(true,"Motivo")));
        verify(usuarios,never()).save(any());
    }
}
