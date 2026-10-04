package com.techprotech.agenda.modulos.plataforma.api.dto;

public record ConfiguracionComercioTenantResponse(
        boolean checkoutConfigurado,
        boolean checkoutHabilitado,
        boolean productosHabilitados,
        boolean serviciosHabilitados,
        String moneda,
        boolean enviosConfigurados,
        boolean enviosHabilitados,
        String proveedorEnvio,
        String ambienteEnvio) {}
