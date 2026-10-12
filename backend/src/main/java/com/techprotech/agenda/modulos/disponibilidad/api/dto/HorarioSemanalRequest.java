package com.techprotech.agenda.modulos.disponibilidad.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record HorarioSemanalRequest(@NotNull @Valid ReglaDisponibilidadRequest horario,
 @NotEmpty @Size(max=7) List<@NotNull @Min(1) @Max(7) Integer> diasSemana,boolean reemplazar) {}
