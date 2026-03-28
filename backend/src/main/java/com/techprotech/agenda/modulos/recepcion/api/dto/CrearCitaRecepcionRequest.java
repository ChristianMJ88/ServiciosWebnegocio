package com.techprotech.agenda.modulos.recepcion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CrearCitaRecepcionRequest(
        @NotNull Long sucursalId,
        @NotNull Long servicioId,
        Long prestadorId,
        @NotBlank @Size(min = 3, max = 150) String nombreCliente,
        @Email @NotBlank String correoCliente,
        @NotBlank @Pattern(regexp = "^[0-9+ ]{10,15}$") String telefonoCliente,
        @NotNull @Future OffsetDateTime inicio,
        @Size(max = 500) String notas
) {
}
