package com.techprotech.agenda.modulos.recepcion.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SolicitudEsperaRecepcionResponse(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        Long servicioId,
        String servicioNombre,
        Long clienteId,
        String nombreCliente,
        String telefonoCliente,
        LocalDate fechaDeseada,
        LocalTime horaDesde,
        LocalTime horaHasta,
        boolean aceptaWhatsapp,
        String canalOrigen,
        String estado,
        String notas,
        LocalDateTime creadaEn,
        LocalDateTime notificadaEn,
        LocalDateTime cerradoEn
) {
}
