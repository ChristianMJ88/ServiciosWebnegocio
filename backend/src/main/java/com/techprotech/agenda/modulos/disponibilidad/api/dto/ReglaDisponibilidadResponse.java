package com.techprotech.agenda.modulos.disponibilidad.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReglaDisponibilidadResponse(
        Long id,
        String tipoSujeto,
        Long sujetoId,
        String sujetoNombre,
        int diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        int intervaloMinutos,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta
) {
}
