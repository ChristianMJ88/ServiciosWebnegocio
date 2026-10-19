package com.techprotech.agenda.modulos.marketing.api;
import com.techprotech.agenda.modulos.marketing.aplicacion.ServicioSoportePlataforma;
import com.techprotech.agenda.modulos.marketing.api.dto.*;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import com.techprotech.agenda.compartido.exception.ManejadorGlobalExcepciones;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.*;import java.util.*;
import static org.mockito.Mockito.*;import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ControladorContactoTest {
 @Configuration @EnableMethodSecurity static class Config {
  @Bean ServicioSoportePlataforma servicio(){return mock(ServicioSoportePlataforma.class);}
  @Bean ControladorSoportePlataforma controlador(ServicioSoportePlataforma s){return new ControladorSoportePlataforma(s);}
 }
 @Test void permisoEfectivoNoNombreRol(){try(var ctx=new AnnotationConfigApplicationContext(Config.class)){
  var c=ctx.getBean(ControladorSoportePlataforma.class);var s=ctx.getBean(ServicioSoportePlataforma.class);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
  assertThatThrownBy(c::obtener).isInstanceOf(AccessDeniedException.class);verifyNoInteractions(s);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operador","",List.of(new SimpleGrantedAuthority("PLATAFORMA_MARKETING_GESTIONAR"))));
  c.obtener();verify(s).obtener();
  var actor=new UsuarioAutenticado("p@example.test",12L,null,"PLATAFORMA",List.of(),List.of(),List.of());
  var v=com.techprotech.agenda.modulos.marketing.aplicacion.ServicioSoportePlataformaTest.valida(4L);
  c.guardar(v,actor);verify(s).guardar(v,12L);
 }finally{SecurityContextHolder.clearContext();}}
 @Test void publicoEsGlobalYNoExponeAuditoriaNiLegalEnSoporte()throws Exception{
  var s=mock(ServicioSoportePlataforma.class);when(s.publico()).thenReturn(new SoportePublicoResponse("help@example.test","+12025550189",List.of(2,4),LocalTime.of(10,0),LocalTime.of(16,0),"Europe/Madrid"));when(s.contactoLegal()).thenReturn(new ContactoLegalResponse("legal@example.test"));
  var mvc=MockMvcBuilders.standaloneSetup(new ControladorContactoPublico(s)).setControllerAdvice(new ManejadorGlobalExcepciones(Clock.systemUTC())).build();
  mvc.perform(get("/api/v1/publico/plataforma/soporte").header("X-Tenant-Id","999")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","max-age=60, public")).andExpect(jsonPath("$.correo").value("help@example.test")).andExpect(jsonPath("$.version").doesNotExist()).andExpect(jsonPath("$.actualizadoPor").doesNotExist()).andExpect(jsonPath("$.correoLegal").doesNotExist());
  mvc.perform(get("/api/v1/publico/plataforma/contacto-legal")).andExpect(status().isOk()).andExpect(content().json("{\"correo\":\"legal@example.test\"}",true)).andExpect(header().string("Cache-Control","max-age=60, public"));
  when(s.publico()).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,"Contacto no disponible"));mvc.perform(get("/api/v1/publico/plataforma/soporte")).andExpect(status().isNotFound()).andExpect(header().doesNotExist("Cache-Control"));
 }
 @Test void cuerpoMalFormadoYCorreoLegalAusenteSon400()throws Exception{
  var service=mock(ServicioSoportePlataforma.class);
  var mvc=MockMvcBuilders.standaloneSetup(new ControladorSoportePlataforma(service)).setControllerAdvice(new ManejadorSolicitudSoporte(),new ManejadorGlobalExcepciones(Clock.systemUTC())).build();
  mvc.perform(put("/api/v1/plataforma/marketing/soporte").contentType("application/json").content("{\"horaInicio\":\"25:00\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION_INVALIDA"));
  mvc.perform(put("/api/v1/plataforma/marketing/soporte").contentType("application/json").content("{\"correo\":\"h@example.test\",\"telefono\":\"+12025550189\",\"dias\":[1],\"horaInicio\":\"10:00\",\"horaFin\":\"16:00\",\"zonaHoraria\":\"Europe/Madrid\"}")).andExpect(status().isBadRequest());
  verifyNoInteractions(service);
 }

}
