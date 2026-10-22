package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.sitio.api.ControladorSitioPublico;
import com.techprotech.agenda.modulos.sitio.aplicacion.ServicioSitioPublico;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;import static org.assertj.core.api.Assertions.*;
class CacheLecturaDominiosTest {
 @Test void noCacheaLaDecisionNiUnaDenegacion(){var servicio=mock(ServicioSitioPublico.class);var controller=new ControladorSitioPublico(servicio);var a=new MockHttpServletResponse();controller.obtenerPorSlug("fixture",a);assertThat(a.getHeader("Cache-Control")).isEqualTo("no-store");when(servicio.obtenerPorHostname("marca.mx")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));var b=new MockHttpServletResponse();assertThatThrownBy(()->controller.obtenerPorHostname("marca.mx",b)).isInstanceOf(ResponseStatusException.class);assertThat(b.getHeader("Cache-Control")).isEqualTo("no-store");}
}
