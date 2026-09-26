package com.techprotech.agenda.modulos.pagos.dominio;

import java.math.BigDecimal;
import java.util.Map;

public record SolicitudCobro(
        Long empresaId,
        String referenciaOrden,
        String idempotencia,
        BigDecimal importe,
        String moneda,
        BigDecimal comisionPlataforma,
        String tokenMedioPago,
        String correoPagador,
        Map<String, String> metadatos
) {
}
