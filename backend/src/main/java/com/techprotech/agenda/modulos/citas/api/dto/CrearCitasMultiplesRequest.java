package com.techprotech.agenda.modulos.citas.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CrearCitasMultiplesRequest(
        @NotNull Long empresaId,
        @NotNull Long sucursalId,
        @NotBlank @Size(min = 3, max = 150) String nombreCliente,
        @Email @NotBlank String correoCliente,
        @NotBlank @Pattern(regexp = "^[0-9+ ]{10,15}$") String telefonoCliente,
        @Size(max = 500) String notas,
        @NotEmpty @Size(max = 20) List<@Valid CrearCitaMultipleItemRequest> items
) {
}
