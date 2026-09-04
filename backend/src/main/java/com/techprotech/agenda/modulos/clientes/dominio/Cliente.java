package com.techprotech.agenda.modulos.clientes.dominio;

import java.time.LocalDateTime;

public record Cliente(
        Long id,
        String nombreCompleto,
        String correo,
        String telefono,
        boolean aceptaWhatsapp,
        String notas,
        long totalCitas,
        LocalDateTime ultimaCita
) {
}
