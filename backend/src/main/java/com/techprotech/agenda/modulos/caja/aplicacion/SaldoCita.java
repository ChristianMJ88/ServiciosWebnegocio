package com.techprotech.agenda.modulos.caja.aplicacion;

import java.math.BigDecimal;

public record SaldoCita(BigDecimal precio, BigDecimal pagadoCaja, BigDecimal pagadoVenta,
        BigDecimal condonado, BigDecimal pendiente, EstadoCobroCita estadoCobro, boolean cobroPointEnCurso) {
    public SaldoCita(BigDecimal precio, BigDecimal pagadoCaja, BigDecimal pagadoVenta, BigDecimal condonado, BigDecimal pendiente, EstadoCobroCita estadoCobro) {
        this(precio,pagadoCaja,pagadoVenta,condonado,pendiente,estadoCobro,false);
    }
    public BigDecimal pagado() { return pagadoCaja.add(pagadoVenta); }
}
