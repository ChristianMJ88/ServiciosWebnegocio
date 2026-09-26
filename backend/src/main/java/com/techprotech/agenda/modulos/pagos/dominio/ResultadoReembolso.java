package com.techprotech.agenda.modulos.pagos.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ResultadoReembolso(
        String referenciaExterna,
        EstadoPago estado,
        BigDecimal importe,
        OffsetDateTime actualizadoEn
) {
}
