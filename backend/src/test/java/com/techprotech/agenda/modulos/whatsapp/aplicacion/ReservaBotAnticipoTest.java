package com.techprotech.agenda.modulos.whatsapp.aplicacion;

import com.techprotech.agenda.modulos.citas.aplicacion.*;
import com.techprotech.agenda.modulos.citas.api.dto.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioConfigEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ReservaBotAnticipoTest {
    @Test void anticipoUsaEnlaceDelTenantConPreseleccionYSinReservar() {
        var citas=mock(ServicioCitas.class);var pagos=mock(ServicioPoliticaReservaPago.class);var sitios=mock(EmpresaSitioConfigRepositorio.class);
        var s=new ServicioOrientacionReservaBot(citas,pagos,sitios,"https://fluora.test/");
        var sitio=new EmpresaSitioConfigEntidad();sitio.setSlug("negocio-a");sitio.setPublicado(true);
        when(sitios.findById(7L)).thenReturn(Optional.of(sitio));when(pagos.puedeCobrarOnline(7L)).thenReturn(true);
        when(citas.requierePagoBot(7L,3L,9L,null)).thenReturn(true);
        assertThat(s.requiereAnticipo(7L,3L,9L)).isTrue();
        assertThat(s.mensaje(7L,3L,9L)).contains("https://fluora.test/e/negocio-a/agendar?sucursal=3&servicios=9");
        sitio.setDominioPrincipal("reservas.negocio-a.test");
        assertThat(s.mensaje(7L,3L,9L)).contains("https://reservas.negocio-a.test/agendar?sucursal=3&servicios=9");
        assertThat(s.mensaje(8L,3L,9L)).doesNotContain("https:").contains("contacta al negocio");
        verify(citas,never()).crearCitaBot(any());verify(sitios).findById(8L);
    }
    @Test void sitioInactivoOCheckoutIndisponibleNoOfreceEnlace() {
        var sitios=mock(EmpresaSitioConfigRepositorio.class);var pagos=mock(ServicioPoliticaReservaPago.class);
        var s=new ServicioOrientacionReservaBot(mock(ServicioCitas.class),pagos,sitios,"https://fluora.test");
        var sitio=new EmpresaSitioConfigEntidad();sitio.setSlug("a");sitio.setPublicado(false);when(sitios.findById(7L)).thenReturn(Optional.of(sitio));
        assertThat(s.mensaje(7L,3L,9L)).doesNotContain("https:");sitio.setPublicado(true);
        assertThat(s.mensaje(7L,3L,9L)).doesNotContain("https:");
    }
    @Test void guardaBotUsaPrecioNetoYPoliticaComunAntesDeCrearNada() {
        var citas=mock(ServicioCitas.class,CALLS_REAL_METHODS);var cotizador=mock(ServicioCotizacionCitas.class);var servicios=mock(ServicioRepositorio.class);
        ReflectionTestUtils.setField(citas,"cotizador",cotizador);ReflectionTestUtils.setField(citas,"servicioRepositorio",servicios);
        var servicio=new ServicioEntidad();servicio.setRequiereAnticipo(true);servicio.setAnticipoTipo("PORCENTAJE");servicio.setAnticipoValor(new BigDecimal("50"));
        when(servicios.findByIdAndEmpresaIdAndActivoTrue(9L,7L)).thenReturn(Optional.of(servicio));
        var linea=new ResultadoCotizacionCitas.LineaCotizacionCita(9L,"Prueba",new BigDecimal("250"),new BigDecimal("50"),new BigDecimal("200"),new BigDecimal("100"),true);
        var resultado=new ResultadoCotizacionCitas("MXN",new BigDecimal("250"),new BigDecimal("50"),new BigDecimal("200"),new BigDecimal("100"),null,null,List.of(linea));when(cotizador.cotizarPrecio(eq(7L),eq(3L),isNull(),any())).thenReturn(resultado);
        assertThatThrownBy(()->citas.crearCitaBot(new CrearCitaRequest(7L,3L,9L,null,"Prueba","prueba@example.test","2220000000",OffsetDateTime.parse("2026-12-01T10:00:00-06:00"),null)))
            .isInstanceOf(AnticipoNoDisponibleException.class).extracting(e->((AnticipoNoDisponibleException)e).getStatusCode().value()).isEqualTo(409);
        servicio.setRequiereAnticipo(false);assertThat(citas.requierePagoBot(7L,3L,9L,null)).isFalse();
    }
}
