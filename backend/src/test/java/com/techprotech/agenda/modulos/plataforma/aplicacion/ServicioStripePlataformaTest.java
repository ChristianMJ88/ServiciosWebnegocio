package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServicioStripePlataformaTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void conservaLaPruebaCuandoStripeEmiteLaFacturaInicialSinCobro() throws Exception {
        JsonNode factura = objectMapper.readTree("{\"amount_paid\":0}");

        assertThat(ServicioStripePlataforma.debeActivarPorFactura(factura, "PRUEBA")).isFalse();
    }

    @Test
    void activaLaSuscripcionCuandoLaFacturaPosteriorTieneUnCobro() throws Exception {
        JsonNode factura = objectMapper.readTree("{\"amount_paid\":19900}");

        assertThat(ServicioStripePlataforma.debeActivarPorFactura(factura, "PRUEBA")).isTrue();
    }

    @Test
    void obtieneElPeriodoDesdeLosItemsDeLaVersionActualDeStripe() throws Exception {
        JsonNode suscripcion = objectMapper.readTree("{\"items\":{\"data\":[{\"current_period_end\":1791757160}]}}");

        assertThat(ServicioStripePlataforma.epochPeriodoActual(suscripcion)).isEqualTo(1791757160L);
    }
}
