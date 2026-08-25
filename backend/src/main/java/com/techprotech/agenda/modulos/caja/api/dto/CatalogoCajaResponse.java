package com.techprotech.agenda.modulos.caja.api.dto;

import java.util.List;

public record CatalogoCajaResponse(
        Long sucursalActivaId,
        List<SucursalCajaCatalogoResponse> sucursales,
        List<OpcionCajaResponse> metodosPago,
        List<OpcionCajaResponse> tiposMovimiento,
        List<OpcionCajaResponse> estadosSesion,
        String estadoSesionAbierta,
        String estadoSesionCerrada,
        String metodoPagoEfectivo
) {
    public record OpcionCajaResponse(String codigo, String etiqueta) {
    }
}
