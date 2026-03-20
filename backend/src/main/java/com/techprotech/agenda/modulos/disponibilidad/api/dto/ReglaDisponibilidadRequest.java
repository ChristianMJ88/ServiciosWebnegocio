package com.techprotech.agenda.modulos.disponibilidad.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReglaDisponibilidadRequest(
        @NotBlank String tipoSujeto,
        @NotNull Long sujetoId,
        @Min(1) @Max(7) int diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @Min(5) int intervaloMinutos,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta
) {
}
