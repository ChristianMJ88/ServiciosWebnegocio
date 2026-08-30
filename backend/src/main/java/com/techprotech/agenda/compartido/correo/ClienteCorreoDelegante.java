package com.techprotech.agenda.compartido.correo;

import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ClienteCorreoDelegante implements ClienteCorreoSaliente {

    private final ClienteCorreoSmtp clienteCorreoSmtp;
    private final ClienteCorreoGraph clienteCorreoGraph;
    private final ClienteCorreoSendgrid clienteCorreoSendgrid;
    private final ClienteCorreoGmail clienteCorreoGmail;

    public ClienteCorreoDelegante(
            ClienteCorreoSmtp clienteCorreoSmtp,
            ClienteCorreoGraph clienteCorreoGraph,
            ClienteCorreoSendgrid clienteCorreoSendgrid,
            ClienteCorreoGmail clienteCorreoGmail
    ) {
        this.clienteCorreoSmtp = clienteCorreoSmtp;
        this.clienteCorreoGraph = clienteCorreoGraph;
        this.clienteCorreoSendgrid = clienteCorreoSendgrid;
        this.clienteCorreoGmail = clienteCorreoGmail;
    }

    @Override
    public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
        if (configuracion.proveedor() == ProveedorCorreo.SENDGRID) {
            clienteCorreoSendgrid.enviar(configuracion, mensaje);
            return;
        }
        try {
            if (configuracion.proveedor() == ProveedorCorreo.GMAIL) {
                clienteCorreoGmail.enviar(configuracion, mensaje);
            } else if (configuracion.proveedor() == ProveedorCorreo.GRAPH) {
                clienteCorreoGraph.enviar(configuracion, mensaje);
            } else {
                clienteCorreoSmtp.enviar(configuracion, mensaje);
            }
        } catch (MailException | IllegalStateException ex) {
            ConfiguracionCorreoResolvida canonica = clienteCorreoSendgrid.configuracionCanonica();
            if (!canonica.habilitado()) {
                throw ex;
            }
            clienteCorreoSendgrid.enviar(
                    canonica,
                    new MensajeCorreoSaliente(
                            mensaje.destinatario(), mensaje.asunto(), mensaje.textoPlano(), mensaje.contenidoHtml(),
                            canonica.responderA(), mensaje.adjuntos()
                    )
            );
        }
    }
}
