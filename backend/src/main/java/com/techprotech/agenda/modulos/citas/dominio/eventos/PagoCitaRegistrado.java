package com.techprotech.agenda.modulos.citas.dominio.eventos;

import java.math.BigDecimal;

/** monto corresponde solo a esta cita, sin productos ni envío de una venta mixta. */
public record PagoCitaRegistrado(DatosEventoCita datos, BigDecimal monto, String moneda,
        TipoRegistro tipoRegistro, Long pagoId, Long ventaId, Long checkoutOrdenId) implements EventoCita {
    public enum TipoRegistro { PAGO_CITA, PAGO_VENTA }
}
