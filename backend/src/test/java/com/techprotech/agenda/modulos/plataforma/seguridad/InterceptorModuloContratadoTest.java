package com.techprotech.agenda.modulos.plataforma.seguridad;

import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import java.util.List;
import static org.mockito.Mockito.*;

class InterceptorModuloContratadoTest {
    static class Controlador { public void operar() {} }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    @Test void protegeOperacionSinAnotacionDeModulo() throws Exception { verificar("/api/v1/admin/citas",false,false); }
    @Test void permiteRegularizarSinSuscripcion() throws Exception { verificar("/api/v1/admin/suscripcion/checkout",true,false); }
    @Test void noBloqueaWebhooksPublicos() throws Exception { verificar("/api/v1/publico/plataforma/stripe/webhook",true,false); }
    @Test void noAplicaAlPersonalDePlataforma() throws Exception { verificar("/api/v1/admin/citas",true,true); }
    private void verificar(String ruta,boolean permitido,boolean plataforma) throws Exception {
        var servicio=mock(ServicioCapacidadesEmpresa.class);
        var usuario=new UsuarioAutenticado("test@example.com",1L,3L,plataforma?"PLATAFORMA":"EMPRESA",List.of(),List.of(),List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario,null,List.of()));
        new InterceptorModuloContratado(servicio).preHandle(new MockHttpServletRequest("GET",ruta),new MockHttpServletResponse(),new HandlerMethod(new Controlador(),Controlador.class.getMethod("operar")));
        if(permitido)verifyNoInteractions(servicio);else verify(servicio).exigirSuscripcion(3L);
    }
}
