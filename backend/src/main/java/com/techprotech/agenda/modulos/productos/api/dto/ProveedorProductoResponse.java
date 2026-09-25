package com.techprotech.agenda.modulos.productos.api.dto;

public record ProveedorProductoResponse(
        Long id, String nombre, String clave, String contacto, String correo,
        String telefono, String sitioWeb, String notas, boolean activo
) {}
