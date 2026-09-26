package com.techprotech.agenda.modulos.pagos.dominio;

public record EventoPagoProveedor(
        ProveedorPago proveedor,
        String eventoExternoId,
        String tipo,
        String referenciaPagoExterna,
        EstadoPago estado,
        boolean produccion
) {
}
