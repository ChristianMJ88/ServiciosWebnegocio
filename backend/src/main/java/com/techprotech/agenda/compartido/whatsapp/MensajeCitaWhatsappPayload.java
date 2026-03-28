package com.techprotech.agenda.compartido.whatsapp;

import java.time.LocalDateTime;

public record MensajeCitaWhatsappPayload(
        Long citaId,
        String telefonoDestino,
        LocalDateTime inicioEsperado
) {
}
