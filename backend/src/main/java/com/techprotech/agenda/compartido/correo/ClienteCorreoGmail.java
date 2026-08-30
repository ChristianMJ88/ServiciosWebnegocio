package com.techprotech.agenda.compartido.correo;

import jakarta.mail.internet.MimeMessage;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

@Component
public class ClienteCorreoGmail {
    private static final String ENVIO = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
    private final ServicioOAuthCorreoGoogle oauth;
    private final RestClient restClient;

    public ClienteCorreoGmail(ServicioOAuthCorreoGoogle oauth, RestClient.Builder builder) {
        this.oauth = oauth;
        this.restClient = builder.build();
    }

    public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
        try {
            MimeMessage mime = new JavaMailSenderImpl().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(configuracion.remitente(), configuracion.nombreRemitente());
            helper.setTo(mensaje.destinatario());
            helper.setSubject(mensaje.asunto());
            helper.setText(mensaje.textoPlano(), mensaje.contenidoHtml());
            if (mensaje.responderA() != null && !mensaje.responderA().isBlank()) helper.setReplyTo(mensaje.responderA());
            if (mensaje.adjuntos() != null) {
                for (AdjuntoCorreoSaliente adjunto : mensaje.adjuntos()) {
                    helper.addAttachment(adjunto.nombreArchivo(),
                            new org.springframework.core.io.ByteArrayResource(adjunto.contenido()), adjunto.tipoContenido());
                }
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            mime.writeTo(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
            restClient.post().uri(ENVIO).contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(oauth.obtenerAccessToken(configuracion.empresaId())))
                    .body(Map.of("raw", raw)).retrieve().toBodilessEntity();
        } catch (Exception ex) {
            throw new MailSendException("No se pudo enviar el correo mediante Gmail", ex);
        }
    }
}
