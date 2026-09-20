package com.techprotech.agenda.modulos.sitio.api.dto;

public record BeneficioSitioAdminResponse(
        Long id,
        String titulo,
        String descripcion,
        String icono,
        boolean activo,
        int orden
) {
}
