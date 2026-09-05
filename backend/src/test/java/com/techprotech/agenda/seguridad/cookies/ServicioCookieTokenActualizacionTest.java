package com.techprotech.agenda.seguridad.cookies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.autenticacion.api.dto.RespuestaTokenJwt;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioCookieTokenActualizacionTest {

    @Test
    void creaCookieHttpOnlySeguraYRestringidaALasRutasDeAuth() {
        ServicioCookieTokenActualizacion servicio = new ServicioCookieTokenActualizacion(true, "None", 7);
        MockHttpServletResponse response = new MockHttpServletResponse();

        servicio.agregar(response, sesion());

        String cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("fluora_refresh=refresh-secreto"));
        assertTrue(cookie.contains("Path=/api/v1/auth"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=None"));
    }

    @Test
    void elRefreshTokenNoSeSerializaEnJson() throws Exception {
        String json = new ObjectMapper().writeValueAsString(sesion());

        assertFalse(json.contains("refresh-secreto"));
        assertFalse(json.contains("tokenActualizacion"));
        assertTrue(json.contains("tokenAcceso"));
    }

    private RespuestaTokenJwt sesion() {
        return new RespuestaTokenJwt(
                "access", "refresh-secreto", "Bearer", 1L, 2L,
                "empresa", "Empresa", true, List.of("ADMIN"), List.of(), List.of()
        );
    }
}
