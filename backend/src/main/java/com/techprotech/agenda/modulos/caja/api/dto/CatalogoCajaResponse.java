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
    public record OpcionCajaResponse(String codigo,String etiqueta,boolean esEfectivo,boolean esPoint,boolean requiereReferencia,
            String ayuda,java.util.Map<String,List<java.math.BigDecimal>> denominaciones) {
        public OpcionCajaResponse(String codigo,String etiqueta) {
            this(codigo,etiqueta,com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.METODO_PAGO_EFECTIVO.equals(codigo),
                com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.METODO_PAGO_POINT.equals(codigo),com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.METODOS_PAGO.stream().filter(m -> m.codigo().equals(codigo)).anyMatch(m -> m.requiereReferencia()),
                "TARJETA".equals(codigo)?"Cobra en una terminal externa y captura la referencia del comprobante.":"TRANSFERENCIA".equals(codigo)?"Verifica el depósito y registra su referencia.":null,java.util.Map.of());
        }
    }
}
