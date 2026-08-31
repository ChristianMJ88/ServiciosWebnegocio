package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record UsuarioInternoAdminRequest(
        Long sucursalId,
        List<Long> sucursalIds,
        @Email @NotBlank @Pattern(regexp = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$", message = "El correo debe incluir un dominio válido") String correo,
        @Size(min = 8, max = 100) String contrasenaTemporal,
        @NotBlank @Size(max = 150) String nombreCompleto,
        @Size(max = 30) String telefono,
        @Size(max = 80) String puesto,
        Long rolEmpresaId,
        List<String> permisosDirectos,
        boolean activo,
        @Size(max = 500) String notas
) {
}
