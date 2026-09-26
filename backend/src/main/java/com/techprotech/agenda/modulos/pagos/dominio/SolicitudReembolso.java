package com.techprotech.agenda.modulos.pagos.dominio;

import java.math.BigDecimal;

public record SolicitudReembolso(
        Long empresaId,
        String referenciaOrdenExterna,
        String referenciaPagoExterna,
        String idempotencia,
        BigDecimal importe,
        String motivo
) {
}
