package com.techprotech.agenda.modulos.disponibilidad.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.techprotech.agenda.modulos.disponibilidad.aplicacion.CatalogosDisponibilidad.INTERVALO_MINIMO_MINUTOS;

public record ReglaDisponibilidadRequest(
        @NotBlank String tipoSujeto,
        @NotNull Long sujetoId,
        @Min(1) @Max(7) int diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @Min(INTERVALO_MINIMO_MINUTOS) int intervaloMinutos,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta
) {
}
