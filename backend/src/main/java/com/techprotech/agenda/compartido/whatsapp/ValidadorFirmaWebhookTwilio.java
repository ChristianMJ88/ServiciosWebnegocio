package com.techprotech.agenda.compartido.whatsapp;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class ValidadorFirmaWebhookTwilio {

    private final PropiedadesWhatsapp propiedades;
    private final ServicioConfiguracionWhatsappEmpresa servicioConfiguracion;

    public ValidadorFirmaWebhookTwilio(
            PropiedadesWhatsapp propiedades,
            ServicioConfiguracionWhatsappEmpresa servicioConfiguracion
    ) {
        this.propiedades = propiedades;
        this.servicioConfiguracion = servicioConfiguracion;
    }

    public void validar(
            Long empresaId,
            HttpServletRequest request,
            MultiValueMap<String, String> parametros,
            String firmaRecibida
    ) {
        if (!propiedades.validarFirmaWebhook()) return;
        if (firmaRecibida == null || firmaRecibida.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta la firma del webhook de Twilio");
        }

        String authToken = servicioConfiguracion.resolver(empresaId).authToken();
        if (authToken == null || authToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay credenciales para validar el webhook de Twilio");
        }

        String firmaEsperada = calcularFirma(urlPublica(request), parametros, authToken);
        boolean valida = MessageDigest.isEqual(
                firmaEsperada.getBytes(StandardCharsets.UTF_8),
                firmaRecibida.trim().getBytes(StandardCharsets.UTF_8)
        );
        if (!valida) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma de webhook de Twilio invalida");
        }
    }

    String calcularFirma(String url, MultiValueMap<String, String> parametros, String authToken) {
        StringBuilder datos = new StringBuilder(url);
        List<String> nombres = new ArrayList<>(parametros.keySet());
        Collections.sort(nombres);
        for (String nombre : nombres) {
            List<String> valores = parametros.get(nombre);
            if (valores == null) continue;
            for (String valor : valores) {
                datos.append(nombre).append(valor == null ? "" : valor);
            }
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(datos.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible validar la firma de Twilio", ex);
        }
    }

    private String urlPublica(HttpServletRequest request) {
        String base = propiedades.webhookPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return request.getRequestURL().toString();
        }
        return base.replaceAll("/+$", "") + request.getRequestURI();
    }
}
