package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RolInternoAdminRequest(
        @NotBlank @Size(max = 80) String codigo,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 255) String descripcion,
        boolean activo,
        List<String> permisos
) {
}
