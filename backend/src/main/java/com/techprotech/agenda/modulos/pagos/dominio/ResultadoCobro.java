package com.techprotech.agenda.modulos.pagos.dominio;

import java.time.OffsetDateTime;

public record ResultadoCobro(
        ProveedorPago proveedor,
        String referenciaExterna,
        EstadoPago estado,
        String accionUrl,
        String codigoRespuesta,
        String detalleRespuesta,
        OffsetDateTime actualizadoEn
) {
}
