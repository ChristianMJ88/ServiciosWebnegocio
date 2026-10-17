package com.techprotech.agenda.modulos.politicas.api;

import com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa;
import com.techprotech.agenda.modulos.politicas.api.dto.*;
import com.techprotech.agenda.modulos.politicas.dominio.ModoNoShow;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ControladorPoliticaTest {
    @Configuration @EnableMethodSecurity static class Config {
        @Bean ServicioPoliticaEmpresa servicio() { return mock(ServicioPoliticaEmpresa.class); }
        @Bean ControladorPoliticaAdmin controlador(ServicioPoliticaEmpresa s) { return new ControladorPoliticaAdmin(s); }
    }
    @Test void permisoEfectivoControlaAccesoSinDependerDeNombreDelRol() {
        try (var ctx = new AnnotationConfigApplicationContext(Config.class)) {
            var c = ctx.getBean(ControladorPoliticaAdmin.class); var s = ctx.getBean(ServicioPoliticaEmpresa.class);
            var actor = new UsuarioAutenticado("a@example.com",1L,7L,"EMPRESA",List.of(),List.of(),List.of());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            assertThatThrownBy(() -> c.obtener(actor)).isInstanceOf(AccessDeniedException.class); verifyNoInteractions(s);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("colaborador","",List.of(new SimpleGrantedAuthority("PARAMETROS_SISTEMA_GESTIONAR"))));
            c.obtener(actor); verify(s).obtener(7L);
        } finally { SecurityContextHolder.clearContext(); }
    }
    @Test void publicoExponeSoloVersionActualSinIdsInternosYCortaCache() throws Exception {
        var s = mock(ServicioPoliticaEmpresa.class);
        when(s.publica("negocio")).thenReturn(new PoliticaPublicaResponse("Negocio",3,OffsetDateTime.parse("2026-10-17T16:00:00Z"),
                ModoNoShow.CREDITO_REAGENDAR,30,90,1,"Texto <script>literal</script>",null,new PoliticaPublicaResponse.Contacto("n@example.com",null)));
        var mvc = MockMvcBuilders.standaloneSetup(new ControladorPoliticaPublica(s)).build();
        mvc.perform(get("/api/v1/publico/empresas/negocio/politicas")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","max-age=60, public"))
                .andExpect(jsonPath("$.version").value(3)).andExpect(jsonPath("$.publicadaPorUsuarioId").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist()).andExpect(jsonPath("$.recordatorio1Dias").doesNotExist());
        verify(s).publica("negocio");
    }
}
