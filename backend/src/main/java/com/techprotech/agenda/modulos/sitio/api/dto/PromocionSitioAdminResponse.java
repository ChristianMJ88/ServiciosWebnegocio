package com.techprotech.agenda.modulos.sitio.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PromocionSitioAdminResponse(
        Long id,
        String titulo,
        String descripcion,
        Long mediaId,
        String imagenUrl,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String tipoDescuento,
        BigDecimal valorDescuento,
        String moneda,
        String textoBoton,
        boolean activa,
        boolean vigente,
        int orden,
        List<Long> servicioIds,
        List<Long> sucursalIds
) {}
