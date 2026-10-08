package com.techprotech.agenda.compartido.correo;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.List;
import java.util.Properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClienteCorreoSmtpTest {
    @Test void enviaVersionesTextoYHtmlEnMultipartAunqueNoHayaAdjuntos() throws Exception {
        var mime=new MimeMessage(Session.getInstance(new Properties()));
        var sender=mock(JavaMailSenderImpl.class);when(sender.createMimeMessage()).thenReturn(mime);
            var config=new ConfiguracionCorreoResolvida(null,true,ProveedorCorreo.SMTP,"sender@example.test","Fluora",null,
                    "smtp.example.test",465,"mailbox@example.test","fixture",true,false,null,null,null,null,null,null,null);
            new ClienteCorreoSmtp(new MailProperties()) {
                @Override protected JavaMailSenderImpl crearMailSender(ConfiguracionCorreoResolvida configuracion){return sender;}
            }.enviar(config,new MensajeCorreoSaliente("recipient@example.test","Prueba","Texto","<p>HTML</p>",null,List.of()));
            mime.saveChanges();
            assertThat(mime.isMimeType("multipart/*")).isTrue();
            verify(sender).send(mime);
    }
}
