package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TenantPlataformaResponse(
        Long id,
        String nombre,
        String slug,
        String estado,
        String zonaHoraria,
        LocalDateTime creadoEn,
        long sucursales,
        long usuarios,
        boolean pagosConectados,
        BigDecimal volumenBrutoConciliado,
        BigDecimal comisionFluora,
        BigDecimal netoConciliado,
        String moneda) {
}
