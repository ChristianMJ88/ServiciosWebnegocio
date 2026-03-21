package com.techprotech.agenda.compartido.correo;

import java.util.List;

public record MensajeCorreoSaliente(
        String destinatario,
        String asunto,
        String textoPlano,
        String contenidoHtml,
        String responderA,
        List<AdjuntoCorreoSaliente> adjuntos
) {
}
