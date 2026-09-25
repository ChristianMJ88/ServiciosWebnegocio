package com.techprotech.agenda.modulos.productos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
        Long id, Long productoId, String productoNombre,
        Long sucursalId, String sucursalNombre, String tipo,
        BigDecimal cantidad, BigDecimal existenciaAnterior, BigDecimal existenciaNueva,
        String referencia, String notas, LocalDateTime creadoEn
) {}
