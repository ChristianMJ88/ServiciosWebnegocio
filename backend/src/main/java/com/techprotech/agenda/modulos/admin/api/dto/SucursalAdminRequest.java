package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SucursalAdminRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 255) String direccion,
        @Pattern(regexp = "^$|^[0-9+ ]{10,15}$") String telefono,
        @NotBlank @Size(max = 60) String zonaHoraria,
        boolean activa
) {
}
