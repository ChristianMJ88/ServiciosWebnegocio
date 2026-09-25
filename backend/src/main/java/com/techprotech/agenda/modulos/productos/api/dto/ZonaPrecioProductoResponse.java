package com.techprotech.agenda.modulos.productos.api.dto;

public record ZonaPrecioProductoResponse(
        Long id, String nombre, String clave, String descripcion, int prioridad, boolean activa
) {}
