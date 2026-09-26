package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapeadorRespuestaMercadoPagoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MapeadorRespuestaMercadoPago mapeador = new MapeadorRespuestaMercadoPago();

    @Test
    void exponeLaAccionDeTresDSCuandoMercadoPagoLaRequiere() throws Exception {
        ResultadoCobro resultado = mapeador.cobro(objectMapper.readTree("""
                {
                  "id":"ORD-3DS",
                  "status":"action_required",
                  "status_detail":"pending_challenge",
                  "transactions":{"payments":[{"payment_method":{"transaction_security":{
                    "url":"https://example.test/3ds"
                  }}}]}
                }
                """));

        assertThat(resultado.estado()).isEqualTo(EstadoPago.REQUIERE_ACCION);
        assertThat(resultado.accionUrl()).isEqualTo("https://example.test/3ds");
    }

    @Test
    void conservaEstadosDesconocidosSinAprobarElCobro() throws Exception {
        ResultadoCobro resultado = mapeador.cobro(objectMapper.readTree("""
                {"id":"ORD-NUEVA","status":"future_status","status_detail":"future_detail"}
                """));

        assertThat(resultado.estado()).isEqualTo(EstadoPago.DESCONOCIDO);
    }
}
