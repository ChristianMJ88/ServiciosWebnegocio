package com.techprotech.agenda.modulos.pagos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConciliacionPagoResponse(
        Long id,
        String canal,
        String estado,
        String proveedor,
        String referenciaOrden,
        String referenciaPago,
        String moneda,
        BigDecimal importeBruto,
        BigDecimal comisionProveedorTotal,
        BigDecimal impuestoComisionProveedor,
        BigDecimal comisionPlataforma,
        BigDecimal impuestoComisionPlataforma,
        BigDecimal retenciones,
        BigDecimal importeReembolsado,
        BigDecimal importeNeto,
        boolean liquidacionDefinitiva,
        LocalDateTime liquidadoEn,
        LocalDateTime actualizadoEn
) {
}
