package com.techprotech.agenda.modulos.productos.api.dto;

public record CategoriaProductoResponse(
        Long id, Long categoriaPadreId, String nombre, String slug,
        String descripcion, int orden, boolean activa
) {}
