package com.techprotech.agenda.modulos.productos.api.dto;

import java.util.List;

public record CatalogoDescuentosProductoResponse(
        String monedaPredeterminada,
        List<OpcionDescuento> productos, List<OpcionDescuento> categorias,
        List<OpcionDescuento> zonasPrecio, List<OpcionDescuento> sucursales,
        List<ReglaDescuentoProductoResponse> reglas
) { public record OpcionDescuento(Long id, String nombre) {} }
