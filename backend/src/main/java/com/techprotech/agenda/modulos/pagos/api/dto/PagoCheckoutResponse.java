package com.techprotech.agenda.modulos.pagos.api.dto;

import java.math.BigDecimal;

public record PagoCheckoutResponse(
        String ordenId,
        String estadoOrden,
        String estadoPago,
        String proveedor,
        String referenciaExterna,
        String referenciaPagoExterna,
        BigDecimal importe,
        String moneda,
        String accionUrl,
        String codigoRespuesta,
        String detalleRespuesta
) {
}
