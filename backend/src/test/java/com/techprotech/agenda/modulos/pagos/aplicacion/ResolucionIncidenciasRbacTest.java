package com.techprotech.agenda.modulos.pagos.aplicacion;
import com.techprotech.agenda.modulos.pagos.api.ControladorConciliacionPagos;
import com.techprotech.agenda.modulos.pagos.api.dto.ResolverIncidenciaRequest;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ResolucionIncidenciasRbacTest {
    @Configuration @EnableMethodSecurity static class Config {
        @Bean ServicioResolucionIncidenciasCheckout resolucion() {return mock(ServicioResolucionIncidenciasCheckout.class);}
        @Bean ControladorConciliacionPagos controlador(ServicioResolucionIncidenciasCheckout r) {return new ControladorConciliacionPagos(mock(ServicioConciliacionPagos.class),mock(ServicioIncidenciasCheckout.class),r);}
    }
    @Test void exigePermisoPagosExistenteParaResolver() {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            var controlador=context.getBean(ControladorConciliacionPagos.class);var servicio=context.getBean(ServicioResolucionIncidenciasCheckout.class);
            var usuario=new UsuarioAutenticado("admin@test.local",9L,7L,"EMPRESA",List.of(),List.of(),List.of());
            var request=new ResolverIncidenciaRequest(ResolverIncidenciaRequest.Tipo.OTRA,"Acuerdo verificado",List.of());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("staff","",List.of()));
            assertThatThrownBy(() -> controlador.resolver(usuario,"orden",request)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(servicio);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority(com.techprotech.agenda.modulos.pagos.seguridad.PermisosPagos.CONFIGURAR))));
            controlador.resolver(usuario,"orden",request);verify(servicio).resolver(usuario,"orden",request);
        } finally {SecurityContextHolder.clearContext();}
    }
}
