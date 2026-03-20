package com.techprotech.agenda.modulos.disponibilidad.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExcepcionDisponibilidadResponse(
        Long id,
        String tipoSujeto,
        Long sujetoId,
        String sujetoNombre,
        LocalDate fechaExcepcion,
        LocalTime horaInicio,
        LocalTime horaFin,
        String tipoBloqueo,
        String motivo
) {
}
