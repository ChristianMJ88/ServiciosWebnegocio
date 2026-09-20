package com.techprotech.agenda.modulos.sitio.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PromocionSitioPublicaResponse(
        Long id,
        String titulo,
        String descripcion,
        String imagenUrl,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String tipoDescuento,
        BigDecimal valorDescuento,
        String moneda,
        String textoBoton,
        List<Long> servicioIds,
        List<String> servicios,
        List<String> sucursales
) {}
