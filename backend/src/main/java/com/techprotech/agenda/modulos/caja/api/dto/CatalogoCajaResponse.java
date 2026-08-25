package com.techprotech.agenda.modulos.caja.api.dto;

import java.util.List;

public record CatalogoCajaResponse(
        Long sucursalActivaId,
        List<SucursalCajaCatalogoResponse> sucursales,
        List<OpcionCajaResponse> metodosPago,
        List<OpcionCajaResponse> tiposMovimiento,
        String estadoSesionAbierta,
        String metodoPagoEfectivo
) {
    public record OpcionCajaResponse(String codigo, String etiqueta) {
    }
}
