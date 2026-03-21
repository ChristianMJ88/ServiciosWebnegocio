package com.techprotech.agenda.compartido.correo;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailSendException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Component
public class ClienteCorreoSmtp {

    private final MailProperties mailProperties;

    public ClienteCorreoSmtp(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
        try {
            JavaMailSenderImpl sender = crearMailSender(configuracion);
            MimeMessage mimeMessage = sender.createMimeMessage();
            boolean tieneAdjuntos = mensaje.adjuntos() != null && !mensaje.adjuntos().isEmpty();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, tieneAdjuntos, "UTF-8");
            helper.setTo(mensaje.destinatario());
            helper.setSubject(mensaje.asunto());
            helper.setText(mensaje.textoPlano(), mensaje.contenidoHtml());
            helper.setFrom(new InternetAddress(
                    configuracion.remitente(),
                    resolverNombreRemitente(configuracion),
                    "UTF-8"
            ));

            if (mensaje.responderA() != null && !mensaje.responderA().isBlank()) {
                helper.setReplyTo(mensaje.responderA());
            }

            if (tieneAdjuntos) {
                for (AdjuntoCorreoSaliente adjunto : mensaje.adjuntos()) {
                    helper.addAttachment(
                            adjunto.nombreArchivo(),
                            new ByteArrayResource(adjunto.contenido()),
                            adjunto.tipoContenido()
                    );
                }
            }

            sender.send(mimeMessage);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new MailSendException("No se pudo construir el mensaje de correo", ex);
        }
    }

    private JavaMailSenderImpl crearMailSender(ConfiguracionCorreoResolvida configuracion) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(configuracion.smtpHost());
        sender.setPort(configuracion.smtpPort());
        sender.setUsername(configuracion.smtpUsername());
        sender.setPassword(configuracion.smtpPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties properties = new Properties();
        properties.putAll(mailProperties.getProperties());
        properties.put("mail.smtp.auth", String.valueOf(configuracion.smtpAuth()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(configuracion.smtpStartTls()));
        sender.setJavaMailProperties(properties);
        return sender;
    }

    private String resolverNombreRemitente(ConfiguracionCorreoResolvida configuracion) {
        if (configuracion.nombreRemitente() != null && !configuracion.nombreRemitente().isBlank()) {
            return configuracion.nombreRemitente();
        }
        return "Agenda";
    }
}
