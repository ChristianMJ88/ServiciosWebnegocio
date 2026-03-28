package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioInternoAdminRequest(
        Long sucursalId,
        @Email @NotBlank String correo,
        @Size(min = 8, max = 100) String contrasenaTemporal,
        @NotBlank @Size(max = 150) String nombreCompleto,
        @Size(max = 30) String telefono,
        @Size(max = 80) String puesto,
        @NotBlank String rolCodigo,
        boolean activo,
        @Size(max = 500) String notas
) {
}
