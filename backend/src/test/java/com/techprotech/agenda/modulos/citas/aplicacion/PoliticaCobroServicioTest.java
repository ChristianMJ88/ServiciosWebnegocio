package com.techprotech.agenda.modulos.citas.aplicacion;
import com.techprotech.agenda.modulos.servicios.infraestructura.entidad.ServicioEntidad;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PoliticaCobroServicioTest {
    @Test void sinCobroIgnoraLaModalidadGlobal() {
        var servicio=new ServicioEntidad();
        assertThat(PoliticaCobroServicio.resolver(new BigDecimal("200"),servicio).importeRequerido()).isZero();
        assertThat(CalculadorAnticipoServicio.calcular(new BigDecimal("200"),servicio,"ANTICIPO_PORCENTAJE",new BigDecimal("50"),null)).isZero();
    }
    @Test void porcentajeFijoYTotalSeCalculanSobreNeto() {
        var servicio=new ServicioEntidad(); servicio.setRequiereAnticipo(true); servicio.setAnticipoTipo("PORCENTAJE"); servicio.setAnticipoValor(new BigDecimal("50"));
        assertThat(PoliticaCobroServicio.resolver(new BigDecimal("200"),servicio).importeRequerido()).isEqualByComparingTo("100");
        servicio.setAnticipoTipo("FIJO"); servicio.setAnticipoValor(new BigDecimal("250"));
        assertThat(PoliticaCobroServicio.resolver(new BigDecimal("200"),servicio).importeRequerido()).isEqualByComparingTo("200");
        servicio.setAnticipoTipo("TOTAL"); servicio.setAnticipoValor(null);
        assertThat(PoliticaCobroServicio.resolver(new BigDecimal("200"),servicio).importeRequerido()).isEqualByComparingTo("200");
    }
    @Test void margenSeEvaluaEnZonaSucursalYReservaTardiaConfirmaHastaInicio() {
        var zona=ZoneId.of("America/Mexico_City"); var inicio=LocalDateTime.of(2026,10,12,14,30);
        var creada=LocalDateTime.of(2026,10,12,20,0); // 14:00 de la sucursal
        var reloj=Clock.fixed(Instant.parse("2026-10-12T20:10:00Z"),ZoneOffset.UTC);
        assertThat(PlazoConfirmacionCita.vencido(inicio,zona,2,creada,reloj)).isFalse();
        assertThat(PlazoConfirmacionCita.limite(inicio,zona,2,creada)).isEqualTo(Instant.parse("2026-10-12T20:30:00Z"));
        assertThat(PlazoConfirmacionCita.vencido(inicio,zona,2,LocalDateTime.of(2026,10,12,17,0),reloj)).isTrue();
        assertThat(PlazoConfirmacionCita.vencido(inicio,zona,2,creada,
                Clock.fixed(Instant.parse("2026-10-12T20:29:59Z"),ZoneOffset.UTC))).isFalse();
        assertThat(PlazoConfirmacionCita.vencido(inicio,zona,2,creada,
                Clock.fixed(Instant.parse("2026-10-12T20:30:00Z"),ZoneOffset.UTC))).isTrue();
    }
}
