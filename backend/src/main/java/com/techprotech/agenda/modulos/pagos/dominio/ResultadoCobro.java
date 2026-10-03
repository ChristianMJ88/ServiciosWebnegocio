package com.techprotech.agenda.modulos.pagos.dominio;

import java.time.OffsetDateTime;

public record ResultadoCobro(
        ProveedorPago proveedor,
        String referenciaExterna,
        String referenciaPagoExterna,
        EstadoPago estado,
        String accionUrl,
        String codigoRespuesta,
        String detalleRespuesta,
        OffsetDateTime actualizadoEn,
        DesgloseLiquidacionPago liquidacion
) {
    public ResultadoCobro(ProveedorPago proveedor, String referenciaExterna, String referenciaPagoExterna,
                          EstadoPago estado, String accionUrl, String codigoRespuesta,
                          String detalleRespuesta, OffsetDateTime actualizadoEn) {
        this(proveedor, referenciaExterna, referenciaPagoExterna, estado, accionUrl, codigoRespuesta,
                detalleRespuesta, actualizadoEn, null);
    }
}
