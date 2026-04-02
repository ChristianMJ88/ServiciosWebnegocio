package com.techprotech.agenda.modulos.recepcion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CrearSolicitudEsperaRecepcionRequest(
        @NotNull Long sucursalId,
        @NotNull Long servicioId,
        Long clienteId,
        @NotBlank @Size(min = 3, max = 150) String nombreCliente,
        @NotBlank @Pattern(regexp = "^[0-9+ ]{10,15}$") String telefonoCliente,
        @NotNull LocalDate fechaDeseada,
        LocalTime horaDesde,
        LocalTime horaHasta,
        boolean aceptaWhatsapp,
        @Size(max = 30) String canalOrigen,
        @Size(max = 500) String notas
) {
}
