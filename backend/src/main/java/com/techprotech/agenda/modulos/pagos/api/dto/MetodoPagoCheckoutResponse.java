package com.techprotech.agenda.modulos.pagos.api.dto;

public record MetodoPagoCheckoutResponse(
        String id,
        String nombre,
        String tipo,
        String estadoProveedor,
        String imagenUrl,
        boolean activo,
        boolean soloProductos,
        int vigenciaDias
) {
}
