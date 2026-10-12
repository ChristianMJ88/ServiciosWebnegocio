package com.techprotech.agenda.modulos.ventasstaff.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaStaffResponse(Long id,String folio,String nombre,String concepto,String estado,String moneda,
    BigDecimal cantidad,BigDecimal total,LocalDateTime fecha) {}
