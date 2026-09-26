package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ValidadorFirmaWebhookMercadoPago {

    public boolean validar(String xSignature, String xRequestId, String dataId, String secreto) {
        if (vacio(xSignature) || vacio(xRequestId) || vacio(dataId) || vacio(secreto)) return false;
        Map<String, String> firma = componentes(xSignature);
        String timestamp = firma.get("ts");
        String hashRecibido = firma.get("v1");
        if (vacio(timestamp) || vacio(hashRecibido)) return false;
        try {
            String plantilla = "id:" + dataId.toLowerCase(Locale.ROOT)
                    + ";request-id:" + xRequestId + ";ts:" + timestamp + ";";
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = hmac.doFinal(plantilla.getBytes(StandardCharsets.UTF_8));
            byte[] recibido = HexFormat.of().parseHex(hashRecibido);
            return MessageDigest.isEqual(esperado, recibido);
        } catch (Exception ex) {
            return false;
        }
    }

    private Map<String, String> componentes(String cabecera) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (String parte : cabecera.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length == 2) valores.put(par[0], par[1]);
        }
        return valores;
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
