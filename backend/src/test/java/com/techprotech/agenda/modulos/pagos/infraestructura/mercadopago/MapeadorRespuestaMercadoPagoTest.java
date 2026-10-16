package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MapeadorRespuestaMercadoPagoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MapeadorRespuestaMercadoPago mapeador = new MapeadorRespuestaMercadoPago(java.time.Clock.systemUTC());

    @Test
    void exponeLaAccionDeTresDSCuandoMercadoPagoLaRequiere() throws Exception {
        ResultadoCobro resultado = mapeador.cobro(objectMapper.readTree("""
                {
                  "id":"ORD-3DS",
                  "status":"action_required",
                  "status_detail":"pending_challenge",
                  "transactions":{"payments":[{"id":"PAY-123","payment_method":{"transaction_security":{
                    "url":"https://example.test/3ds"
                  }}}]}
                }
                """));

        assertThat(resultado.estado()).isEqualTo(EstadoPago.REQUIERE_ACCION);
        assertThat(resultado.referenciaPagoExterna()).isEqualTo("PAY-123");
        assertThat(resultado.accionUrl()).isEqualTo("https://example.test/3ds");
    }

    @Test
    void conservaEstadosDesconocidosSinAprobarElCobro() throws Exception {
        ResultadoCobro resultado = mapeador.cobro(objectMapper.readTree("""
                {"id":"ORD-NUEVA","status":"future_status","status_detail":"future_detail"}
                """));

        assertThat(resultado.estado()).isEqualTo(EstadoPago.DESCONOCIDO);
    }

    @Test
    void conservaElDesgloseRealDeLaLiquidacionSinInventarImpuestos() throws Exception {
        ResultadoCobro resultado = mapeador.cobro(objectMapper.readTree("""
                {
                  "id":"ORD-LIQ-1",
                  "status":"processed",
                  "status_detail":"accredited",
                  "currency":"MXN",
                  "total_amount":"500.00",
                  "last_updated_date":"2026-10-03T12:30:00Z",
                  "transactions":{"payments":[{
                    "id":"PAY-LIQ-1",
                    "fee_details":[
                      {"type":"mercadopago_fee","amount":"18.56"},
                      {"type":"marketplace_fee","amount":"5.00"}
                    ],
                    "transaction_details":{"net_received_amount":"476.44"}
                  }]}
                }
                """));

        assertThat(resultado.liquidacion()).isNotNull();
        assertThat(resultado.liquidacion().importeBruto()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(resultado.liquidacion().comisionProveedorTotal()).isEqualByComparingTo(new BigDecimal("18.56"));
        assertThat(resultado.liquidacion().comisionPlataforma()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(resultado.liquidacion().importeNeto()).isEqualByComparingTo(new BigDecimal("476.44"));
        assertThat(resultado.liquidacion().impuestoComisionProveedor()).isNull();
        assertThat(resultado.liquidacion().definitiva()).isFalse();
        assertThat(resultado.liquidacion().liquidadoEn()).isNull();
    }
}
