package com.techprotech.agenda.modulos.recepcion.api.dto;

import java.util.List;

public record CatalogoRecepcionResponse(
        Long sucursalActivaId,
        List<SucursalRecepcionCatalogoResponse> sucursales,
        List<ServicioRecepcionCatalogoResponse> servicios
) {
}
