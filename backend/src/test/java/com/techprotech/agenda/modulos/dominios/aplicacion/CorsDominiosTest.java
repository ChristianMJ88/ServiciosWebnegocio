package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.compartido.config.ConfiguracionCors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.mockito.Mockito.*;import static org.assertj.core.api.Assertions.*;
class CorsDominiosTest {
 @Test void originPreservadoPorElProxySoloPermiteDominioActivoEnApisPublicas(){
  var consulta=mock(ServicioConsultaDominios.class);ObjectProvider<ServicioConsultaDominios> provider=mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(consulta);var source=new ConfiguracionCors().corsConfigurationSource("https://console.example",provider);when(consulta.empresaActiva("marca.mx")).thenReturn(7L);
  var publicRequest=new MockHttpServletRequest("POST","/api/v1/publico/citas");publicRequest.addHeader("Origin","https://marca.mx");assertThat(source.getCorsConfiguration(publicRequest).checkOrigin("https://marca.mx")).isEqualTo("https://marca.mx");
  assertThat(source.getCorsConfiguration(publicRequest).getAllowCredentials()).isFalse();assertThat(source.getCorsConfiguration(publicRequest).getExposedHeaders()).isEmpty();
  when(consulta.empresaActiva("marca.mx")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));assertThat(source.getCorsConfiguration(publicRequest).checkOrigin("https://marca.mx")).isNull();clearInvocations(consulta);
  var adminRequest=new MockHttpServletRequest("POST","/api/v1/admin/dominios");adminRequest.addHeader("Origin","https://marca.mx");assertThat(source.getCorsConfiguration(adminRequest).checkOrigin("https://marca.mx")).isNull();verifyNoInteractions(consulta);
  for(String ruta:java.util.List.of("/api/v1/auth/iniciar-sesion","/api/v1/auth/refrescar-token","/api/v1/plataforma/dominios")){var denied=new MockHttpServletRequest("POST",ruta);denied.addHeader("Origin","https://marca.mx");assertThat(source.getCorsConfiguration(denied).checkOrigin("https://marca.mx")).isNull();}verifyNoInteractions(consulta);
  for(String origin:java.util.List.of("http://marca.mx","https://marca.mx:443")){var denied=new MockHttpServletRequest("POST","/api/v1/publico/citas");denied.addHeader("Origin",origin);assertThat(source.getCorsConfiguration(denied).checkOrigin(origin)).isNull();}verifyNoInteractions(consulta);
  var platformRequest=new MockHttpServletRequest("GET","/api/v1/admin/clientes");platformRequest.addHeader("Origin","https://console.example");assertThat(source.getCorsConfiguration(platformRequest).getAllowCredentials()).isTrue();assertThat(source.getCorsConfiguration(platformRequest).checkOrigin("https://console.example")).isEqualTo("https://console.example");
 }
}
