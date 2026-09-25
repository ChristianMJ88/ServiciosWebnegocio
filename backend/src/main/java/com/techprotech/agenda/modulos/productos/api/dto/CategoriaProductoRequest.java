package com.techprotech.agenda.modulos.productos.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaProductoRequest(
        Long categoriaPadreId,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 140) String slug,
        @Size(max = 500) String descripcion,
        @Min(0) int orden,
        boolean activa
) {}
