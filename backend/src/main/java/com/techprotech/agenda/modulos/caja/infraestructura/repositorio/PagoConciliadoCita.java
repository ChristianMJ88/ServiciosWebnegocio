package com.techprotech.agenda.modulos.caja.infraestructura.repositorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Proyección de persistencia: registradoEn conserva el DATETIME UTC hasta el mapeo de API. */
public record PagoConciliadoCita(Long id, Long citaId, Long cajaSesionId, BigDecimal monto,
        String metodoPago, String referencia, String observaciones, LocalDateTime registradoEn) {}
