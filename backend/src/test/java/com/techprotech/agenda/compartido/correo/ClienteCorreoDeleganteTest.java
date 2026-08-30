package com.techprotech.agenda.compartido.correo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailSendException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClienteCorreoDeleganteTest {

    @Test
    void usaSendgridCuandoFallaElProveedorDelTenant() {
        ClienteCorreoSmtp smtp = new ClienteCorreoSmtp(new MailProperties());
        ClienteCorreoGraph graph = new ClienteCorreoGraph(RestClient.builder()) {
            @Override
            public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
                throw new MailSendException("Graph no disponible");
            }
        };
        ClienteCorreoSendgridFalso sendgrid = new ClienteCorreoSendgridFalso();
        ClienteCorreoDelegante delegante = new ClienteCorreoDelegante(smtp, graph, sendgrid);
        ConfiguracionCorreoResolvida tenant = configuracion(ProveedorCorreo.GRAPH, "tenant@cliente.com");
        ConfiguracionCorreoResolvida canonica = configuracion(ProveedorCorreo.SENDGRID, "no-reply@refluora.com");
        MensajeCorreoSaliente mensaje = new MensajeCorreoSaliente(
                "cliente@example.com", "Asunto", "Texto", "<p>Texto</p>", "tenant@cliente.com", List.of()
        );

        sendgrid.canonica = canonica;

        delegante.enviar(tenant, mensaje);

        assertEquals(canonica, sendgrid.ultimaConfiguracion);
        assertEquals("contacto@refluora.com", sendgrid.ultimoMensaje.responderA());
    }

    @Test
    void usaSendgridDirectamenteParaElFallbackGlobal() {
        ClienteCorreoSmtp smtp = new ClienteCorreoSmtp(new MailProperties());
        ClienteCorreoGraph graph = new ClienteCorreoGraph(RestClient.builder());
        ClienteCorreoSendgridFalso sendgrid = new ClienteCorreoSendgridFalso();
        ClienteCorreoDelegante delegante = new ClienteCorreoDelegante(smtp, graph, sendgrid);
        ConfiguracionCorreoResolvida canonica = configuracion(ProveedorCorreo.SENDGRID, "no-reply@refluora.com");
        MensajeCorreoSaliente mensaje = new MensajeCorreoSaliente(
                "cliente@example.com", "Asunto", "Texto", "<p>Texto</p>", "contacto@refluora.com", List.of()
        );

        delegante.enviar(canonica, mensaje);

        assertEquals(canonica, sendgrid.ultimaConfiguracion);
        assertEquals(mensaje, sendgrid.ultimoMensaje);
    }

    private ConfiguracionCorreoResolvida configuracion(ProveedorCorreo proveedor, String remitente) {
        return new ConfiguracionCorreoResolvida(
                true, proveedor, remitente, "Fluora", "contacto@refluora.com",
                "smtp.example.com", 587, "usuario", "secreto", true, true,
                "tenant", "client", "secret", remitente, null, null
        );
    }

    private static final class ClienteCorreoSendgridFalso extends ClienteCorreoSendgrid {
        private ConfiguracionCorreoResolvida canonica;
        private ConfiguracionCorreoResolvida ultimaConfiguracion;
        private MensajeCorreoSaliente ultimoMensaje;

        private ClienteCorreoSendgridFalso() {
            super(RestClient.builder(), new PropiedadesCorreoPlataforma(
                    true, "SG.prueba", "no-reply@refluora.com", "Fluora", "contacto@refluora.com"
            ));
        }

        @Override
        public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
            this.ultimaConfiguracion = configuracion;
            this.ultimoMensaje = mensaje;
        }

        @Override
        public ConfiguracionCorreoResolvida configuracionCanonica() {
            return canonica;
        }
    }
}
