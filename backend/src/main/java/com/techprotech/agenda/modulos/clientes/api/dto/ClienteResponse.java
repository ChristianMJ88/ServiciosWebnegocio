package com.techprotech.agenda.modulos.clientes.api.dto;

import java.time.LocalDateTime;

public record ClienteResponse(Long id, String nombreCompleto, String correo, String telefono,
                              boolean aceptaWhatsapp, String notas, long totalCitas, LocalDateTime ultimaCita) {
}
