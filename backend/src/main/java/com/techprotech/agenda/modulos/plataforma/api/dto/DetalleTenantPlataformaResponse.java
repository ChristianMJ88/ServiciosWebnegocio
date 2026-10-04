package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record DetalleTenantPlataformaResponse(
        TenantPlataformaResponse tenant,
        long serviciosActivos,
        long productosActivos,
        long clientes,
        long citasUltimos30Dias,
        long ventasUltimos30Dias,
        BigDecimal importeVentasUltimos30Dias,
        String monedaVentas,
        ConfiguracionComercioTenantResponse comercio,
        List<IntegracionPagoTenantResponse> integracionesPago) {}
