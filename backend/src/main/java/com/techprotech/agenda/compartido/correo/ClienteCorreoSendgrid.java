package com.techprotech.agenda.compartido.correo;

import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClienteCorreoSendgrid {

    private static final String MAIL_SEND_URL = "https://api.sendgrid.com/v3/mail/send";

    private final RestClient restClient;
    private final PropiedadesCorreoPlataforma propiedades;

    public ClienteCorreoSendgrid(RestClient.Builder restClientBuilder, PropiedadesCorreoPlataforma propiedades) {
        this.restClient = restClientBuilder.build();
        this.propiedades = propiedades;
    }

    public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
        if (!propiedades.habilitado() || esVacio(propiedades.sendgridApiKey())) {
            throw new MailSendException("El correo canonico de Fluora no esta configurado");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("personalizations", List.of(Map.of(
                "to", List.of(Map.of("email", mensaje.destinatario())),
                "subject", mensaje.asunto()
        )));
        payload.put("from", identidad(configuracion.remitente(), configuracion.nombreRemitente()));
        if (!esVacio(mensaje.responderA())) {
            payload.put("reply_to", Map.of("email", mensaje.responderA()));
        }
        payload.put("content", List.of(
                Map.of("type", "text/plain", "value", mensaje.textoPlano()),
                Map.of("type", "text/html", "value", mensaje.contenidoHtml())
        ));

        if (mensaje.adjuntos() != null && !mensaje.adjuntos().isEmpty()) {
            List<Map<String, Object>> attachments = new ArrayList<>();
            for (AdjuntoCorreoSaliente adjunto : mensaje.adjuntos()) {
                attachments.add(Map.of(
                        "content", Base64.getEncoder().encodeToString(adjunto.contenido()),
                        "filename", adjunto.nombreArchivo(),
                        "type", adjunto.tipoContenido(),
                        "disposition", "attachment"
                ));
            }
            payload.put("attachments", attachments);
        }

        try {
            restClient.post()
                    .uri(MAIL_SEND_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(propiedades.sendgridApiKey()))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new MailSendException("No se pudo enviar el correo mediante SendGrid", ex);
        }
    }

    public ConfiguracionCorreoResolvida configuracionCanonica() {
        return new ConfiguracionCorreoResolvida(
                null,
                propiedades.habilitado() && !esVacio(propiedades.sendgridApiKey()) && !esVacio(propiedades.remitente()),
                ProveedorCorreo.SENDGRID,
                propiedades.remitente(),
                propiedades.nombreRemitente(),
                propiedades.responderA(),
                null, 0, null, null, false, false,
                null, null, null, null, null, null, null
        );
    }

    private Map<String, String> identidad(String correo, String nombre) {
        if (esVacio(nombre)) {
            return Map.of("email", correo);
        }
        return Map.of("email", correo, "name", nombre);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
