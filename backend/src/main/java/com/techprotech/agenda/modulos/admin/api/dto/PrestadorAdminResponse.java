package com.techprotech.agenda.modulos.admin.api.dto;

import java.util.List;
import java.math.BigDecimal;

public record PrestadorAdminResponse(
        Long usuarioId,
        Long sucursalId,
        String sucursalNombre,
        String correo,
        String nombreMostrar,
        String biografia,
        String fotoUrl,
        String fotoEncuadre,
        BigDecimal fotoZoom,
        String colorAgenda,
        boolean activo,
        List<Long> servicioIds,
        List<String> servicioNombres,
        String nombreUsuario,
        boolean bloqueado
) implements AccesoUsuarioCreadoResponse {
}
