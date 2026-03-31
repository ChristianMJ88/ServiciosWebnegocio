package com.techprotech.agenda.modulos.caja.api.dto;

import java.util.List;

public record CatalogoCajaResponse(
        Long sucursalActivaId,
        List<SucursalCajaCatalogoResponse> sucursales
) {
}
