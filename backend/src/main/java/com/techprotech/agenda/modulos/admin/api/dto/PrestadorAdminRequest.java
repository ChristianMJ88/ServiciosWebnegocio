package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PrestadorAdminRequest(
        @NotNull Long sucursalId,
        @Email @NotBlank String correo,
        @Size(min = 8, max = 100) String contrasenaTemporal,
        @NotBlank @Size(max = 150) String nombreMostrar,
        @Size(max = 500) String biografia,
        @Size(max = 20) String colorAgenda,
        boolean activo,
        List<Long> servicioIds
) {
}
