package com.techprotech.agenda.modulos.pagos.dominio;

public record MetodoPagoProveedor(
        String id,
        String nombre,
        String tipo,
        String estado,
        String imagenUrl
) {
}
