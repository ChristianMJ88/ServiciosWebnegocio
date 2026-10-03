package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.math.BigDecimal;

public record ResumenPlataformaResponse(
        long tenantsTotal,
        long tenantsActivos,
        long usuariosTenant,
        long cuentasPagoConectadas,
        BigDecimal volumenBrutoConciliado,
        BigDecimal comisionesProveedor,
        BigDecimal comisionesFluora,
        BigDecimal netoNegocios,
        String moneda) {
}
