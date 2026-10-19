package com.techprotech.agenda.modulos.marketing.api;

import com.techprotech.agenda.modulos.marketing.aplicacion.ServicioSoportePlataforma;
import com.techprotech.agenda.modulos.marketing.api.dto.ContactoLegalResponse;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.UsuarioEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.UsuarioRepositorio;
import com.techprotech.agenda.seguridad.config.ConfiguracionSeguridad;
import com.techprotech.agenda.seguridad.jwt.*;
import com.techprotech.agenda.seguridad.ratelimit.FiltroLimiteSolicitudes;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Clock;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContactoSeguridadTest {
 @Configuration @EnableWebMvc @EnableWebSecurity @Import(ConfiguracionSeguridad.class)
 static class Config {
  @Bean Clock clock(){return Clock.systemUTC();}
  @Bean ServicioSoportePlataforma servicio(){return mock(ServicioSoportePlataforma.class);}
  @Bean ControladorSoportePlataforma admin(ServicioSoportePlataforma s){return new ControladorSoportePlataforma(s);}
  @Bean ControladorContactoPublico publico(ServicioSoportePlataforma s){return new ControladorContactoPublico(s);}
  @Bean UsuarioRepositorio usuarios(){return mock(UsuarioRepositorio.class);}
  @Bean ServicioTokenJwt jwt(Clock c){return new ServicioTokenJwt(new PropiedadesJwt(UUID.randomUUID().toString().repeat(2),"prueba-contacto",10,1),c);}
  @Bean FiltroAutenticacionJwt filtro(ServicioTokenJwt jwt,UsuarioRepositorio u,Clock c){return new FiltroAutenticacionJwt(jwt,u,c);}
  @Bean FiltroLimiteSolicitudes limite(Clock c){return new FiltroLimiteSolicitudes(false,c);}
 }
 @Test void tokenTenantInclusoConMismoPermisoNoEntraAPlataformaYPublicoEsGlobal()throws Exception {
  try(var ctx=new AnnotationConfigWebApplicationContext()){
   ctx.setServletContext(new MockServletContext());ctx.register(Config.class);ctx.refresh();
   var mvc=MockMvcBuilders.webAppContextSetup(ctx).addFilters((jakarta.servlet.Filter)ctx.getBean("springSecurityFilterChain")).build();
   var jwt=ctx.getBean(ServicioTokenJwt.class);var u=new UsuarioEntidad();u.setHabilitado(true);
   when(ctx.getBean(UsuarioRepositorio.class).findByIdAndEmpresaId(3L,7L)).thenReturn(Optional.of(u));
   var permiso=List.of("PLATAFORMA_MARKETING_GESTIONAR");
   var tenant=jwt.generarTokenAcceso("tenant@example.test",3L,7L,"prueba",List.of(),permiso,List.of());
   var sinPermiso=jwt.generarTokenPlataforma("platform@example.test",12L,List.of(),List.of());
   var conPermiso=jwt.generarTokenPlataforma("platform@example.test",12L,List.of(),permiso);
   String ruta="/api/v1/plataforma/marketing/soporte";
   mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
   var denied=mvc.perform(get(ruta).header("Authorization","Bearer "+tenant)).andExpect(status().isForbidden()).andReturn();
   org.assertj.core.api.Assertions.assertThat(denied.getResponse().getErrorMessage()).isNull();
   org.assertj.core.api.Assertions.assertThat(denied.getResponse().getForwardedUrl()).isNull();
   mvc.perform(put(ruta).header("Authorization","Bearer "+tenant)).andExpect(status().isForbidden());
   mvc.perform(get("/api/v1/plataforma/empresas").header("Authorization","Bearer "+tenant)).andExpect(status().isForbidden());
   mvc.perform(get(ruta).header("Authorization","Bearer "+sinPermiso)).andExpect(status().isForbidden());
   verifyNoInteractions(ctx.getBean(ServicioSoportePlataforma.class));
   mvc.perform(get(ruta).header("Authorization","Bearer "+conPermiso)).andExpect(status().isOk());
   var s=ctx.getBean(ServicioSoportePlataforma.class);when(s.contactoLegal()).thenReturn(new ContactoLegalResponse("legal@example.test"));
   for(var token:List.of("",tenant))mvc.perform(get("/api/v1/publico/plataforma/contacto-legal").header("Authorization",token.isEmpty()?"":"Bearer "+token).header("X-Tenant-Id","999")).andExpect(status().isOk()).andExpect(content().json("{\"correo\":\"legal@example.test\"}",true));
  }
 }
}
