package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrupoServicioAdminRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 140) String slug,
        @Size(max = 300) String descripcion,
        @Size(max = 255) String imagenUrl,
        @Size(max = 50) String icono,
        @Min(0) int ordenPublico,
        boolean activo
) {
}
