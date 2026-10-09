package com.techprotech.agenda.modulos.marketing.aplicacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.marketing.api.dto.LandingMarketingDto;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.LandingMarketingEntidad;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.LandingMarketingRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioAuditoriaPlataforma;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ServicioLandingMarketingTest {
 final LandingMarketingRepositorio repo=mock(LandingMarketingRepositorio.class);
 final ServicioAuditoriaPlataforma auditoria=mock(ServicioAuditoriaPlataforma.class);
 final ObjectMapper json=new ObjectMapper();
 final ServicioLandingMarketingImpl service=new ServicioLandingMarketingImpl(repo,json,auditoria);
 LandingMarketingDto contenido(String imagen,Long version){var b=new LandingMarketingDto.Bloque("Agenda","Organiza citas");return new LandingMarketingDto("barberias",true,version,"Fluora","Agenda","Servicios","Crear cuenta","Cómo funciona","Comienza",imagen,null,"Título SEO","Descripción SEO",List.of(b),List.of(b),List.of(b),List.of());}
 @Test void borradorNoSeExpone(){when(repo.findBySlugAndPublicadoTrue("barberias")).thenReturn(Optional.empty());assertEquals(404,assertThrows(ResponseStatusException.class,()->service.publica("barberias")).getStatusCode().value());}
 @Test void rechazaImagenConEsquemaEjecutable(){assertEquals(400,assertThrows(ResponseStatusException.class,()->service.guardar("barberias",contenido("javascript:alert(1)",null),1L)).getStatusCode().value());verifyNoInteractions(repo,auditoria);}
 @Test void rechazaVersionDesactualizada(){var e=new LandingMarketingEntidad();e.setId(1L);e.setVersion(2L);when(repo.findBySlug("barberias")).thenReturn(Optional.of(e));assertEquals(409,assertThrows(ResponseStatusException.class,()->service.guardar("barberias",contenido(null,1L),1L)).getStatusCode().value());verify(repo,never()).saveAndFlush(any());}
 @Test void publicaUsaEstadoYVersionPersistidos() throws Exception{var e=new LandingMarketingEntidad();e.setSlug("barberias");e.setPublicado(true);e.setVersion(3L);e.setContenido(json.writeValueAsString(contenido(null,0L)));when(repo.findBySlugAndPublicadoTrue("barberias")).thenReturn(Optional.of(e));assertEquals(3L,service.publica("barberias").version());}
 @Test void guardaYAuditaSinDatosDeTenant(){when(repo.findBySlug("barberias")).thenReturn(Optional.empty());when(repo.saveAndFlush(any())).thenAnswer(i->{var e=(LandingMarketingEntidad)i.getArgument(0);e.setId(1L);e.setVersion(0L);return e;});var result=service.guardar("barberias",contenido("https://images.example.test/hero.webp",null),2L);assertTrue(result.publicado());verify(auditoria).registrar(eq(2L),eq("LANDING_GUARDAR"),eq("LANDING"),eq("barberias"),anyMap());}
}
