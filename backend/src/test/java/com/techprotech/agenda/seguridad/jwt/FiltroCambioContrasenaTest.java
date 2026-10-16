package com.techprotech.agenda.seguridad.jwt;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FiltroCambioContrasenaTest {
    @ParameterizedTest
    @ValueSource(strings = {"inactivo", "bloqueado", "revocado", "ausente"})
    void rechazaAccesoRevocadoAunqueElTokenSigaFirmado(String estado) throws Exception {
        var tokens = mock(ServicioTokenJwt.class);
        var usuarios = mock(UsuarioRepositorio.class);
        var cadena = mock(FilterChain.class);
        var usuario = new UsuarioEntidad();
        usuario.setHabilitado(!estado.equals("inactivo"));
        usuario.setBloqueado(estado.equals("bloqueado"));
        usuario.setVersionSesion(estado.equals("revocado") ? 1L : 0L);
        when(tokens.esValido("token")).thenReturn(true);
        when(tokens.obtenerAmbito("token")).thenReturn("TENANT");
        when(tokens.obtenerUsuarioId("token")).thenReturn(20L);
        when(tokens.obtenerEmpresaId("token")).thenReturn(10L);
        when(usuarios.findByIdAndEmpresaId(20L,10L)).thenReturn(estado.equals("ausente") ? Optional.empty() : Optional.of(usuario));
        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer token");
        var response = new MockHttpServletResponse();
        new FiltroAutenticacionJwt(tokens, usuarios, java.time.Clock.systemUTC()).doFilter(request, response, cadena);
        assertEquals(401, response.getStatus()); verifyNoInteractions(cadena);
        var respuesta = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
        assertEquals("SESION_NO_DISPONIBLE", respuesta.get("codigo").asText());
        org.junit.jupiter.api.Assertions.assertFalse(respuesta.get("mensaje").asText().isBlank());
    }
    @Test
    void bloqueaTokenAnteriorCuandoAdministradorRestableceContrasena() throws Exception {
        var tokens = mock(ServicioTokenJwt.class);
        var usuarios = mock(UsuarioRepositorio.class);
        var cadena = mock(FilterChain.class);
        var usuario = new UsuarioEntidad();
        usuario.setHabilitado(true);
        usuario.setRequiereCambioContrasena(true);
        when(tokens.esValido("token-anterior")).thenReturn(true);
        when(tokens.obtenerAmbito("token-anterior")).thenReturn("TENANT");
        when(tokens.obtenerUsuarioId("token-anterior")).thenReturn(20L);
        when(tokens.obtenerEmpresaId("token-anterior")).thenReturn(10L);
        when(usuarios.findByIdAndEmpresaId(20L, 10L)).thenReturn(Optional.of(usuario));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-anterior");
        var response = new MockHttpServletResponse();
        new FiltroAutenticacionJwt(tokens, usuarios, java.time.Clock.systemUTC()).doFilter(request, response, cadena);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(cadena);
    }
}
