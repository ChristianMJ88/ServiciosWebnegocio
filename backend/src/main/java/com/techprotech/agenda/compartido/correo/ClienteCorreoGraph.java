package com.techprotech.agenda.compartido.correo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.UUID;

@Component
public class ClienteCorreoGraph {

    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    private static final String GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0";
    private static final String LOGIN_BASE_URL = "https://login.microsoftonline.com";
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final RestClient restClient;

    public ClienteCorreoGraph(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
        try {
            String accessToken = obtenerAccessToken(configuracion);
            String userIdCodificado = URLEncoder.encode(configuracion.graphUserId(), StandardCharsets.UTF_8);

            List<Map<String, Object>> attachments = new ArrayList<>();
            if (mensaje.adjuntos() != null) {
                for (AdjuntoCorreoSaliente adjunto : mensaje.adjuntos()) {
                    attachments.add(Map.of(
                            "@odata.type", "#microsoft.graph.fileAttachment",
                            "name", adjunto.nombreArchivo(),
                            "contentType", adjunto.tipoContenido(),
                            "contentBytes", Base64.getEncoder().encodeToString(adjunto.contenido())
                    ));
                }
            }

            Map<String, Object> message = new java.util.LinkedHashMap<>();
            message.put("subject", mensaje.asunto());
            message.put("body", Map.of(
                    "contentType", "HTML",
                    "content", mensaje.contenidoHtml()
            ));
            message.put("toRecipients", List.of(
                    Map.of("emailAddress", Map.of("address", mensaje.destinatario()))
            ));
            message.put("replyTo", mensaje.responderA() != null && !mensaje.responderA().isBlank()
                    ? List.of(Map.of("emailAddress", Map.of("address", mensaje.responderA())))
                    : List.of());
            if (!attachments.isEmpty()) {
                message.put("attachments", attachments);
            }

            Map<String, Object> payload = Map.of(
                    "message", message,
                    "saveToSentItems", false
            );

            restClient.post()
                    .uri(GRAPH_BASE_URL + "/users/" + userIdCodificado + "/sendMail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new org.springframework.mail.MailSendException("No se pudo enviar el correo mediante Microsoft Graph", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String obtenerAccessToken(ConfiguracionCorreoResolvida configuracion) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        String tokenEndpoint = LOGIN_BASE_URL + "/" + configuracion.graphTenantId() + "/oauth2/v2.0/token";
        form.add("grant_type", "client_credentials");
        form.add("client_id", configuracion.graphClientId());
        form.add("scope", GRAPH_SCOPE);

        if (configuracion.graphCertificateThumbprint() != null
                && !configuracion.graphCertificateThumbprint().isBlank()
                && configuracion.graphPrivateKeyPem() != null
                && !configuracion.graphPrivateKeyPem().isBlank()) {
            form.add("client_assertion_type", CLIENT_ASSERTION_TYPE);
            form.add("client_assertion", construirClientAssertion(configuracion, tokenEndpoint));
        } else {
            form.add("client_secret", configuracion.graphClientSecret());
        }

        Map<String, Object> response = restClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Microsoft Graph no devolvio respuesta al solicitar el token");
        }

        Object accessToken = response.get("access_token");
        if (!(accessToken instanceof String token) || token.isBlank()) {
            throw new IllegalStateException("Microsoft Graph no devolvio un access_token valido");
        }

        return token;
    }

    private String construirClientAssertion(ConfiguracionCorreoResolvida configuracion, String tokenEndpoint) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "RS256")
                .add("x5t", convertirThumbprintABase64Url(configuracion.graphCertificateThumbprint()))
                .and()
                .issuer(configuracion.graphClientId())
                .subject(configuracion.graphClientId())
                .audience().add(tokenEndpoint).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(java.util.Date.from(ahora))
                .notBefore(java.util.Date.from(ahora.minusSeconds(30)))
                .expiration(java.util.Date.from(ahora.plusSeconds(600)))
                .signWith(cargarLlavePrivada(configuracion.graphPrivateKeyPem()), SignatureAlgorithm.RS256)
                .compact();
    }

    private RSAPrivateKey cargarLlavePrivada(String pem) {
        try {
            String contenido = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decodificado = Base64.getDecoder().decode(contenido);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodificado);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey llave = keyFactory.generatePrivate(keySpec);
            return (RSAPrivateKey) llave;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cargar la llave privada PEM de Microsoft Graph", ex);
        }
    }

    private String convertirThumbprintABase64Url(String thumbprintHex) {
        try {
            byte[] bytes = hexStringToBytes(thumbprintHex.replace(" ", ""));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo convertir el thumbprint del certificado Graph", ex);
        }
    }

    private byte[] hexStringToBytes(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
