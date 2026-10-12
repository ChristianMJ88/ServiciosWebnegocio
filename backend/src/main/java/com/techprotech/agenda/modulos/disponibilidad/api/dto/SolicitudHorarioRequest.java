package com.techprotech.agenda.modulos.disponibilidad.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
public record SolicitudHorarioRequest(Long reglaId, @NotNull @Valid HorarioSemanalRequest propuesta) {}
