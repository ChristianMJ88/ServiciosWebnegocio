package com.techprotech.agenda.modulos.marketing.aplicacion;
import com.techprotech.agenda.modulos.marketing.api.dto.AtribucionAdquisicionRequest;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.AdquisicionEmpresaEntidad;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.LandingMarketingEntidad;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.AdquisicionEmpresaRepositorio;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.LandingMarketingRepositorio;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ServicioAdquisicionEmpresaTest {
 private final AdquisicionEmpresaRepositorio repo=mock(AdquisicionEmpresaRepositorio.class);
 private final LandingMarketingRepositorio landings=mock(LandingMarketingRepositorio.class);
 private final ServicioAdquisicionEmpresaImpl servicio=new ServicioAdquisicionEmpresaImpl(repo,landings);
 @Test void registroSinCampanaNoCreaHistoriaArtificial(){servicio.registrar(7L,null);verifyNoInteractions(repo,landings);}
 @Test void rutaYFechaSonDelServidorYEmpresaEsLaRecienCreada(){
  when(landings.findBySlug("unas")).thenReturn(Optional.of(new LandingMarketingEntidad()));
  servicio.registrar(7L,new AtribucionAdquisicionRequest("google","cpc","unas_octubre",null,null,"unas"));
  var captor=ArgumentCaptor.forClass(AdquisicionEmpresaEntidad.class);verify(repo).save(captor.capture());
  assertEquals(7L,captor.getValue().getEmpresaId());assertEquals("/unas",captor.getValue().getFirstLandingPath());
  assertEquals("unas_octubre",captor.getValue().getCampaign());assertNotNull(captor.getValue().getAcquiredAt());
 }
 @Test void descartaDatosPersonalesYVerticalDesconocida(){
  when(landings.findBySlug("desconocida")).thenReturn(Optional.empty());
  servicio.registrar(7L,new AtribucionAdquisicionRequest("persona@example.com",null,"telefono5512345678",null,null,"desconocida"));
  verifyNoInteractions(repo);
 }
}
