package com.techprotech.agenda.modulos.caja.aplicacion;

import java.math.BigDecimal;

public record SaldoCita(BigDecimal precio, BigDecimal pagadoCaja, BigDecimal pagadoVenta,
        BigDecimal condonado, BigDecimal pendiente, EstadoCobroCita estadoCobro) {
    public BigDecimal pagado() { return pagadoCaja.add(pagadoVenta); }
}
