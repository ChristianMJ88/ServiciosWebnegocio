package com.techprotech.agenda.modulos.sitio.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ActualizarServiciosDestacadosRequest(
        @NotBlank String vista,
        @NotNull Boolean mostrarPrecio,
        @NotNull Boolean mostrarDuracion,
        @NotNull Boolean mostrarDescripcion,
        @Min(1) @Max(12) int limite,
        @NotNull List<@Valid ServicioDestacadoSeleccionRequest> elementos
) {
}
