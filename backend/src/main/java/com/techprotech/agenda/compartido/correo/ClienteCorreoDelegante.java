package com.techprotech.agenda.compartido.correo;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ClienteCorreoDelegante implements ClienteCorreoSaliente {

    private final ClienteCorreoSmtp clienteCorreoSmtp;
    private final ClienteCorreoGraph clienteCorreoGraph;

    public ClienteCorreoDelegante(
            ClienteCorreoSmtp clienteCorreoSmtp,
            ClienteCorreoGraph clienteCorreoGraph
    ) {
        this.clienteCorreoSmtp = clienteCorreoSmtp;
        this.clienteCorreoGraph = clienteCorreoGraph;
    }

    @Override
    public void enviar(ConfiguracionCorreoResolvida configuracion, MensajeCorreoSaliente mensaje) {
        if (configuracion.proveedor() == ProveedorCorreo.GRAPH) {
            clienteCorreoGraph.enviar(configuracion, mensaje);
            return;
        }

        clienteCorreoSmtp.enviar(configuracion, mensaje);
    }
}
