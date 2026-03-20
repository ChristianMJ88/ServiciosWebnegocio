package com.techprotech.agenda.modulos.disponibilidad.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExcepcionDisponibilidadRequest(
        @NotBlank String tipoSujeto,
        @NotNull Long sujetoId,
        @NotNull LocalDate fechaExcepcion,
        LocalTime horaInicio,
        LocalTime horaFin,
        @NotBlank String tipoBloqueo,
        @Size(max = 255) String motivo
) {
}
