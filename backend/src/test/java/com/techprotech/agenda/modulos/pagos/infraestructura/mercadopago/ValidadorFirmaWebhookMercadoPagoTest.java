package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorFirmaWebhookMercadoPagoTest {
    private final ValidadorFirmaWebhookMercadoPago validador = new ValidadorFirmaWebhookMercadoPago();

    @Test
    void validaLaPlantillaOficialSinDependerDelOrdenDeLaCabecera() throws Exception {
        String secreto = "secreto-webhook";
        String dataId = "ORD01ABC";
        String requestId = "request-123";
        String timestamp = "1742505638683";
        String plantilla = "id:ord01abc;request-id:request-123;ts:1742505638683;";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hash = HexFormat.of().formatHex(mac.doFinal(plantilla.getBytes(StandardCharsets.UTF_8)));

        assertTrue(validador.validar("v1=" + hash + ",ts=" + timestamp, requestId, dataId, secreto));
        assertFalse(validador.validar("ts=" + timestamp + ",v1=" + hash, requestId, "otra-orden", secreto));
    }
}
