package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.admin.api.ControladorAdminCitas;
import com.techprotech.agenda.modulos.admin.aplicacion.ServicioAdminCitas;
import com.techprotech.agenda.modulos.prestadores.api.ControladorAgendaStaff;
import com.techprotech.agenda.modulos.prestadores.aplicacion.ServicioAgendaStaff;
import com.techprotech.agenda.modulos.recepcion.api.ControladorRecepcion;
import com.techprotech.agenda.modulos.recepcion.aplicacion.ServicioRecepcion;
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

class CierreCitaRbacTest {
    @Configuration @EnableMethodSecurity static class Config {
        @Bean ServicioRecepcion recepcion() {return mock(ServicioRecepcion.class);}
        @Bean ServicioAgendaStaff staff() {return mock(ServicioAgendaStaff.class);}
        @Bean ServicioAdminCitas admin() {return mock(ServicioAdminCitas.class);}
        @Bean ControladorRecepcion controladorRecepcion(ServicioRecepcion s) {return new ControladorRecepcion(s);}
        @Bean ControladorAgendaStaff controladorStaff(ServicioAgendaStaff s) {return new ControladorAgendaStaff(s);}
        @Bean ControladorAdminCitas controladorAdmin(ServicioAdminCitas s) {return new ControladorAdminCitas(s);}
    }
    @Test void lasTresRutasExigenSuPermisoEfectivoYNoElNombreDeRolNiPermisoDeCobro() {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            var r=context.getBean(ControladorRecepcion.class);var s=context.getBean(ControladorAgendaStaff.class);var a=context.getBean(ControladorAdminCitas.class);
            var usuario=new UsuarioAutenticado(null,11L,7L,"EMPRESA",List.of("ADMIN"),List.of(),List.of(3L));
            autenticar("CAJA_COBRAR");
            assertThatThrownBy(() -> r.finalizar(usuario,1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> r.noAsistio(usuario,1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> s.finalizar(1L,usuario)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> s.noAsistio(1L,usuario)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> a.finalizarCita(usuario,1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> a.marcarNoAsistio(usuario,1L)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(context.getBean(ServicioRecepcion.class),context.getBean(ServicioAgendaStaff.class),context.getBean(ServicioAdminCitas.class));
            autenticar("RECEPCION_CITAS_GESTIONAR");r.finalizar(usuario,1L);r.noAsistio(usuario,1L);
            verify(context.getBean(ServicioRecepcion.class)).finalizar(usuario,1L);verify(context.getBean(ServicioRecepcion.class)).noAsistio(usuario,1L);
            autenticar("STAFF_CITAS_GESTIONAR");assertThat(s.finalizar(1L,usuario).getStatusCode().value()).isEqualTo(200);s.noAsistio(1L,usuario);
            verify(context.getBean(ServicioAgendaStaff.class)).cambiarEstado(usuario,1L,"FINALIZADA");verify(context.getBean(ServicioAgendaStaff.class)).cambiarEstado(usuario,1L,"NO_ASISTIO");
            autenticar("CITAS_ADMIN_GESTIONAR");assertThat(a.finalizarCita(usuario,1L).getStatusCode().value()).isEqualTo(200);a.marcarNoAsistio(usuario,1L);
            verify(context.getBean(ServicioAdminCitas.class)).cambiarEstadoCita(usuario,1L,"FINALIZADA");verify(context.getBean(ServicioAdminCitas.class)).cambiarEstadoCita(usuario,1L,"NO_ASISTIO");
        } finally {SecurityContextHolder.clearContext();}
    }
    static void autenticar(String permiso) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("rol personalizado","",List.of(new SimpleGrantedAuthority(permiso))));
    }
}
