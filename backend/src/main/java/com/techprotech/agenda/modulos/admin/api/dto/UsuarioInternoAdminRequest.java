package com.techprotech.agenda.modulos.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record UsuarioInternoAdminRequest(
        Long sucursalId,
        List<Long> sucursalIds,
        @Email @Size(max=150) String correo,
        @Size(min = 8, max = 100) String contrasenaTemporal,
        @NotBlank @Size(max = 150) String nombreCompleto,
        @Size(max = 30) String telefono,
        @Size(max = 80) String puesto,
        Long rolEmpresaId,
        List<String> permisosDirectos,
        boolean activo,
        @Size(max = 500) String notas,
        @Pattern(regexp="[a-zA-Z0-9._-]{3,80}") String nombreUsuario
) {
 public UsuarioInternoAdminRequest(Long sucursalId,List<Long> sucursalIds,String correo,String contrasenaTemporal,String nombreCompleto,String telefono,String puesto,Long rolEmpresaId,List<String> permisosDirectos,boolean activo,String notas){this(sucursalId,sucursalIds,correo,contrasenaTemporal,nombreCompleto,telefono,puesto,rolEmpresaId,permisosDirectos,activo,notas,null);}
}
