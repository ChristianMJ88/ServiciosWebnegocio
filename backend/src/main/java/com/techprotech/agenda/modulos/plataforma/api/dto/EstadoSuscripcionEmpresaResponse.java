package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EstadoSuscripcionEmpresaResponse(
        boolean tieneSuscripcion,
        Long suscripcionId,
        Long planId,
        String planNombre,
        String estado,
        String ciclo,
        BigDecimal precioAcordado,
        String moneda,
        LocalDateTime iniciaEn,
        LocalDateTime pruebaHasta,
        LocalDateTime periodoActualHasta,
        boolean cancelarAlFinalPeriodo,
        boolean administrableStripe
) {
    public static EstadoSuscripcionEmpresaResponse sinSuscripcion() {
        return new EstadoSuscripcionEmpresaResponse(false, null, null, null, null, null, null, null,
                null, null, null, false, false);
    }
}
