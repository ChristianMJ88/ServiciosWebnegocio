package com.techprotech.agenda.modulos.productos.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorProductoRequest(
        @NotBlank @Size(max = 160) String nombre,
        @Size(max = 60) String clave,
        @Size(max = 160) String contacto,
        @Email @Size(max = 180) String correo,
        @Size(max = 30) String telefono,
        @Size(max = 500) String sitioWeb,
        @Size(max = 1000) String notas,
        boolean activo
) {}
