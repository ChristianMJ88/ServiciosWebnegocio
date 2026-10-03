package com.techprotech.agenda.modulos.pagos.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Importes informados por el proveedor. Los valores desconocidos permanecen en null. */
public record DesgloseLiquidacionPago(
        String moneda,
        BigDecimal importeBruto,
        BigDecimal comisionProveedorTotal,
        BigDecimal impuestoComisionProveedor,
        BigDecimal comisionPlataforma,
        BigDecimal impuestoComisionPlataforma,
        BigDecimal retenciones,
        BigDecimal importeReembolsado,
        BigDecimal importeNeto,
        boolean definitiva,
        OffsetDateTime liquidadoEn
) {
}
