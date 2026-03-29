package com.techprotech.agenda.modulos.admin.api.dto;

import java.util.List;

public record PlantillaRolInternoAdminResponse(
        String codigoSugerido,
        String nombreSugerido,
        String descripcion,
        String categoria,
        List<String> permisos
) {
}
