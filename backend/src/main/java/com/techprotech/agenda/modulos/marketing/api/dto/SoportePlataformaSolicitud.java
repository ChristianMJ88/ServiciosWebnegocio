package com.techprotech.agenda.modulos.marketing.api.dto;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.List;
public record SoportePlataformaSolicitud(
    @PositiveOrZero Long version,
    @NotBlank @Size(max=150) String correo,
    @NotBlank @Size(max=150) String correoLegal,
    @NotBlank @Size(max=64) String telefono,
    @NotNull @Size(min=1,max=7) List<@NotNull @Min(1) @Max(7) Integer> dias,
    @NotNull LocalTime horaInicio, @NotNull LocalTime horaFin,
    @NotBlank @Size(max=64) String zonaHoraria, boolean visible
) {}
