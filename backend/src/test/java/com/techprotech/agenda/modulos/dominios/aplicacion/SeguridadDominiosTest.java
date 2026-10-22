package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.dominios.api.*;
import com.techprotech.agenda.modulos.dominios.api.dto.*;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
class SeguridadDominiosTest {
 @Configuration @EnableMethodSecurity static class Config {
  @Bean ServicioDominios servicio(){return mock(ServicioDominios.class);}
  @Bean ControladorDominiosAdmin admin(ServicioDominios s){return new ControladorDominiosAdmin(s);}
  @Bean ControladorDominiosPlataforma plataforma(ServicioDominios s){return new ControladorDominiosPlataforma(s);}
 }
 @Test void permisosDeCadaAmbitoSinCompararRoles(){try(var ctx=new AnnotationConfigApplicationContext(Config.class)){
  var admin=ctx.getBean(ControladorDominiosAdmin.class);var plataforma=ctx.getBean(ControladorDominiosPlataforma.class);var servicio=ctx.getBean(ServicioDominios.class);
  var actor=new UsuarioAutenticado("fixture@example.test",3L,7L,"EMPRESA",List.of(),List.of(),List.of());
  autenticar("OTRO_PERMISO");assertThatThrownBy(()->admin.panel(actor)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThatThrownBy(()->plataforma.retirar(actor,1L,new VersionDominioSolicitud(0L))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(servicio);
  autenticar("CONFIGURACION_EMPRESA_GESTIONAR");admin.panel(actor);verify(servicio).panel(7L);assertThatThrownBy(()->plataforma.retirar(actor,1L,new VersionDominioSolicitud(0L))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  autenticar("PLATAFORMA_DOMINIOS_GESTIONAR");plataforma.retirar(actor,1L,new VersionDominioSolicitud(0L));verify(servicio).retirarPlataforma(3L,1L,0L);
 }finally{SecurityContextHolder.clearContext();}}
 private void autenticar(String permiso){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("fixture","",List.of(new SimpleGrantedAuthority(permiso))));}
}
