package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioOAuthMercadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.EstadoPago;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoCobro;
import com.techprotech.agenda.modulos.pagos.dominio.ResultadoReembolso;
import com.techprotech.agenda.modulos.pagos.dominio.SolicitudCobro;
import com.techprotech.agenda.modulos.pagos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AdaptadorMercadoPagoOrdersTest {
    private MockRestServiceServer servidor;
    private AdaptadorMercadoPagoOrders adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        ServicioOAuthMercadoPago oauth = mock(ServicioOAuthMercadoPago.class);
        when(oauth.obtenerAccessToken(12L)).thenReturn("token-de-prueba");
        ObjectMapper objectMapper = new ObjectMapper();
        adaptador = new AdaptadorMercadoPagoOrders(oauth, builder, objectMapper,
                new MapeadorRespuestaMercadoPago(), new ValidadorFirmaWebhookMercadoPago(),
                new PropiedadesMercadoPago(true, "PRUEBA", "cliente", "secreto", "redirect", "frontend", "webhook",
                        "publica-prueba", "token-prueba"));
    }

    @Test
    void creaOrdenAutomaticaConIdempotenciaYTokenDelTenant() {
        servidor.expect(once(), requestTo("https://api.mercadopago.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andExpect(header("X-Idempotency-Key", "idem-venta-42"))
                .andExpect(content().json("""
                        {
                          "type":"online",
                          "processing_mode":"automatic",
                          "capture_mode":"automatic",
                          "total_amount":"80.00",
                          "external_reference":"VENTA-42",
                          "payer":{"email":"cliente@example.com"},
                          "transactions":{"payments":[{"amount":"80.00","payment_method":{
                            "id":"visa","type":"credit_card","token":"card-token","installments":1
                          }}]}
                        }
                        """, false))
                .andRespond(withSuccess("""
                        {
                          "id":"ORD-42",
                          "status":"processed",
                          "status_detail":"accredited",
                          "last_updated_date":"2026-09-26T15:30:00Z"
                        }
                        """, MediaType.APPLICATION_JSON));

        ResultadoCobro resultado = adaptador.crearCobro(new SolicitudCobro(
                12L, "VENTA-42", "idem-venta-42", new BigDecimal("80"), "MXN", BigDecimal.ZERO,
                "card-token", "cliente@example.com",
                Map.of("payment_method_id", "visa", "payment_type_id", "credit_card", "installments", "1")));

        assertThat(resultado.referenciaExterna()).isEqualTo("ORD-42");
        assertThat(resultado.estado()).isEqualTo(EstadoPago.APROBADO);
        servidor.verify();
    }

    @Test
    void resuelveElTipoDeTarjetaDesdeElCatalogoDelTenantCuandoElBrickNoLoEnvia() {
        servidor.expect(once(), requestTo("https://api.mercadopago.com/v1/payment_methods"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andRespond(withSuccess("""
                        [{"id":"master","name":"Mastercard","payment_type_id":"credit_card"}]
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo("https://api.mercadopago.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"transactions":{"payments":[{"payment_method":{"id":"master","type":"credit_card"}}]}}
                        """, false))
                .andRespond(withSuccess("""
                        {"id":"ORD-43","status":"processed","status_detail":"accredited"}
                        """, MediaType.APPLICATION_JSON));

        ResultadoCobro resultado = adaptador.crearCobro(new SolicitudCobro(
                12L, "VENTA-43", "idem-venta-43", new BigDecimal("185"), "MXN", BigDecimal.ZERO,
                "card-token", "cliente@example.com",
                Map.of("payment_method_id", "master", "installments", "1")));

        assertThat(resultado.referenciaExterna()).isEqualTo("ORD-43");
        servidor.verify();
    }

    @Test
    void creaPagoReferenciadoConVigenciaSinTokenDeTarjeta() {
        servidor.expect(once(), requestTo("https://api.mercadopago.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.capture_mode").doesNotExist())
                .andExpect(content().json("""
                        {
                          "type":"online",
                          "processing_mode":"automatic",
                          "expiration_time":"P3D",
                          "payer":{"email":"cliente@example.com"},
                          "transactions":{"payments":[{"payment_method":{"id":"metodo-dinamico","type":"ticket"}}]}
                        }
                        """, false))
                .andRespond(withSuccess("""
                        {
                          "id":"ORD-REF-1",
                          "status":"action_required",
                          "status_detail":"waiting_payment",
                          "transactions":{"payments":[{"id":"PAY-REF-1","payment_method":{"ticket_url":"https://proveedor.example/instrucciones"}}]}
                        }
                        """, MediaType.APPLICATION_JSON));

        ResultadoCobro resultado = adaptador.crearCobro(new SolicitudCobro(
                12L, "VENTA-REF-1", "idem-ref-1", new BigDecimal("250"), "MXN", BigDecimal.ZERO,
                null, "cliente@example.com", Map.of("payment_method_id", "metodo-dinamico",
                "payment_type_id", "ticket", "expiration_time", "P3D")));

        assertThat(resultado.estado()).isEqualTo(EstadoPago.REQUIERE_ACCION);
        assertThat(resultado.accionUrl()).isEqualTo("https://proveedor.example/instrucciones");
        servidor.verify();
    }

    @Test
    void reembolsaUnaTransaccionParcialSinConfundirOrdenYPago() {
        servidor.expect(once(), requestTo("https://api.mercadopago.com/v1/orders/ORD-42/refund"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", "idem-refund-42"))
                .andExpect(content().json("""
                        {"transactions":[{"id":"PAY-42","amount":"25.00"}]}
                        """, true))
                .andRespond(withSuccess("""
                        {
                          "id":"ORD-42",
                          "status":"processed",
                          "status_detail":"partially_refunded",
                          "transactions":{"refunds":[{"id":"REF-42","amount":"25.00","status":"processed"}]}
                        }
                        """, MediaType.APPLICATION_JSON));

        ResultadoReembolso resultado = adaptador.reembolsar(new SolicitudReembolso(
                12L, "ORD-42", "PAY-42", "idem-refund-42", new BigDecimal("25"), "Solicitud del cliente"));

        assertThat(resultado.referenciaExterna()).isEqualTo("REF-42");
        assertThat(resultado.estado()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(resultado.importe()).isEqualByComparingTo("25.00");
        servidor.verify();
    }
}
