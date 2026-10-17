package com.techprotech.agenda.modulos.citas.aplicacion;
import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ConfiguracionReservasResponse;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioConfiguracionCheckout;
import com.techprotech.agenda.modulos.pagos.api.dto.ConfiguracionCheckoutResponse;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
class ServicioPoliticaReservaPagoTest {
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.CsvSource({"false,true,true,true","true,false,true,true","true,true,false,true","true,true,true,false"})
    void todasLasCompuertasDeCobroSeRespetan(boolean habilitado,boolean conectado,boolean servicios,boolean tarjeta) {
        var checkout=mock(ServicioConfiguracionCheckout.class);var politicas=mock(com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa.class);when(politicas.vigente(7L)).thenReturn(java.util.Optional.of(new com.techprotech.agenda.modulos.politicas.infraestructura.entidad.PoliticaEmpresaEntidad()));
        when(checkout.obtener(7L)).thenReturn(new ConfiguracionCheckoutResponse(habilitado,"MERCADO_PAGO",conectado,"PRUEBA",tarjeta,false,false,true,false,servicios,true,false,BigDecimal.ZERO,false,BigDecimal.ZERO,"TOTAL",null,null,"MXN"));
        var s=new ServicioPoliticaReservaPago(checkout,mock(ServicioConfiguracionReservas.class),Clock.systemUTC(),politicas);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->s.plan(7L,true,List.of(BigDecimal.TEN))).isInstanceOf(AnticipoNoDisponibleException.class);
    }
    @Test void sinPoliticaOCheckoutNoDegradaAnticipoAPagoPresencial() {
        var checkout=mock(ServicioConfiguracionCheckout.class); var politicas=mock(com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa.class);
        var s=new ServicioPoliticaReservaPago(checkout,mock(ServicioConfiguracionReservas.class),Clock.systemUTC(),politicas);
        when(checkout.obtener(7L)).thenReturn(new ConfiguracionCheckoutResponse(true,"MERCADO_PAGO",true,"PRUEBA",true,false,false,true,false,true,true,false,BigDecimal.ZERO,false,BigDecimal.ZERO,"TOTAL",null,null,"MXN"));
        org.assertj.core.api.Assertions.assertThatThrownBy(()->s.plan(7L,true,List.of(BigDecimal.ZERO,BigDecimal.TEN))).isInstanceOf(AnticipoNoDisponibleException.class);
        assertThat(s.plan(7L,true,List.of(BigDecimal.ZERO)).requierePago()).isFalse();
        assertThat(s.plan(7L,false,List.of(BigDecimal.TEN)).requierePago()).isFalse();
        when(politicas.vigente(7L)).thenReturn(java.util.Optional.of(new com.techprotech.agenda.modulos.politicas.infraestructura.entidad.PoliticaEmpresaEntidad()));
        when(checkout.obtener(7L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        org.assertj.core.api.Assertions.assertThatThrownBy(()->s.plan(7L,true,List.of(BigDecimal.TEN))).isInstanceOf(AnticipoNoDisponibleException.class);
    }

    @Test void vigenciaSeCalculaDesdeRelojInyectadoYPoliticaDelTenant() {
        var checkout=mock(ServicioConfiguracionCheckout.class);
        var reservas=mock(ServicioConfiguracionReservas.class);
        var config=new ConfiguracionCheckoutResponse(true,"MERCADO_PAGO",true,"PRUEBA",true,false,false,true,false,true,true,false,BigDecimal.ZERO,false,BigDecimal.ZERO,"TOTAL",null,null,"MXN");
        when(checkout.obtener(7L)).thenReturn(config);

        when(reservas.obtener(7L)).thenReturn(new ConfiguracionReservasResponse(0,30,2,17,3));
        var reloj=Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"),ZoneOffset.UTC);
        var politicas=mock(com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa.class);
        when(politicas.vigente(7L)).thenReturn(java.util.Optional.of(new com.techprotech.agenda.modulos.politicas.infraestructura.entidad.PoliticaEmpresaEntidad()));
        var servicio=new ServicioPoliticaReservaPago(checkout,reservas,reloj,politicas);
        assertThat(servicio.plan(7L,true,List.of(new BigDecimal("100"))).expiraEn()).isEqualTo(LocalDateTime.now(reloj).plusMinutes(17));
        assertThat(servicio.plan(7L,false,List.of(new BigDecimal("100"))).requierePago()).isFalse();
        assertThat(servicio.plan(7L,true,List.of(BigDecimal.ZERO)).requierePago()).isFalse();
    }
}
