package com.techprotech.agenda.modulos.productos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExistenciaProductoResponse(
        Long productoId, String sku, String productoNombre,
        Long sucursalId, String sucursalNombre,
        BigDecimal existencia, BigDecimal reservado, BigDecimal disponible,
        BigDecimal stockMinimo, BigDecimal stockMaximo,
        String estado, LocalDateTime actualizadoEn
) {}
