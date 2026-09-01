package com.techprotech.agenda.modulos.autenticacion.social;

import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioAutenticacion;
import com.techprotech.agenda.modulos.autenticacion.aplicacion.ServicioRolesEmpresa;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAccesoSocialTest {
    @Mock UsuarioIdentidadExternaRepositorio identidades;
    @Mock CodigoAccesoSocialRepositorio codigos;
    @Mock UsuarioRepositorio usuarios;
    @Mock ServicioAutenticacion autenticacion;
    @Mock EmpresaRepositorio empresas;
    @Mock ServicioRolesEmpresa roles;

    @Test
    void vinculaCuentaExistentePorCorreoVerificadoSinCambiarEmpresaNiRoles() {
        UsuarioEntidad usuario = new UsuarioEntidad();
        usuario.setId(41L);
        usuario.setEmpresaId(7L);
        usuario.setCorreo("persona@ejemplo.com");
        usuario.setHabilitado(true);
        usuario.setBloqueado(false);

        when(identidades.findAllByProveedorAndSubjectProveedor("GOOGLE", "subject-google"))
                .thenReturn(List.of(), List.of(new UsuarioIdentidadExternaEntidad()));
        when(usuarios.findByCorreoOrderByEmpresaIdAsc("persona@ejemplo.com")).thenReturn(List.of(usuario));
        when(identidades.existsByUsuarioIdAndProveedor(41L, "GOOGLE")).thenReturn(false);
        when(codigos.save(any(CodigoAccesoSocialEntidad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ServicioAccesoSocial servicio = new ServicioAccesoSocial(
                identidades, codigos, usuarios, autenticacion, empresas, roles);

        String codigo = servicio.crearCodigo(new PerfilRegistroSocial(
                "GOOGLE", "subject-google", "Persona@Ejemplo.com", "Persona"));

        ArgumentCaptor<UsuarioIdentidadExternaEntidad> captor =
                ArgumentCaptor.forClass(UsuarioIdentidadExternaEntidad.class);
        verify(identidades).save(captor.capture());
        assertEquals(41L, captor.getValue().getUsuarioId());
        assertEquals("persona@ejemplo.com", captor.getValue().getCorreoVerificado());
        assertNotNull(usuario.getCorreoVerificadoEn());
        assertNotNull(codigo);
    }
}
