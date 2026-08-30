package com.techprotech.agenda.compartido.correo;

import java.time.LocalDateTime;

public record EventoCitaCorreoPayload(Long citaId, LocalDateTime inicioEsperado) {
}
