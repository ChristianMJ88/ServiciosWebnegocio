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
    @Test void vigenciaSeCalculaDesdeRelojInyectadoYPoliticaDelTenant() {
        var checkout=mock(ServicioConfiguracionCheckout.class);
        var reservas=mock(ServicioConfiguracionReservas.class);
        var config=new ConfiguracionCheckoutResponse(true,"MERCADO_PAGO",true,"PRUEBA",true,false,false,true,false,true,true,false,BigDecimal.ZERO,false,BigDecimal.ZERO,"TOTAL",null,null,"MXN");
        when(checkout.obtener(7L)).thenReturn(config);

        when(reservas.obtener(7L)).thenReturn(new ConfiguracionReservasResponse(0,30,2,17,3));
        var reloj=Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"),ZoneOffset.UTC);
        var servicio=new ServicioPoliticaReservaPago(checkout,reservas,reloj);
        assertThat(servicio.plan(7L,true,List.of(new BigDecimal("100"))).expiraEn()).isEqualTo(LocalDateTime.now(reloj).plusMinutes(17));
        assertThat(servicio.plan(7L,false,List.of(new BigDecimal("100"))).requierePago()).isFalse();
        assertThat(servicio.plan(7L,true,List.of(BigDecimal.ZERO)).requierePago()).isFalse();
    }
}
