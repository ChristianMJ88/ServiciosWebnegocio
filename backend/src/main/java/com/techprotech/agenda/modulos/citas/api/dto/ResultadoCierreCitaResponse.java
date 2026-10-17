package com.techprotech.agenda.modulos.citas.api.dto;

import com.techprotech.agenda.modulos.caja.aplicacion.EstadoCobroCita;
import java.math.BigDecimal;

public record ResultadoCierreCitaResponse(Long citaId,Long sucursalId,String estado,BigDecimal precio,
        BigDecimal pagadoCaja,BigDecimal pagadoVenta,BigDecimal pagado,BigDecimal condonado,
        BigDecimal pendiente,EstadoCobroCita estadoCobro,boolean puedeCobrar,String moneda) {}
