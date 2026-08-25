package com.techprotech.agenda.modulos.recepcion.api.dto;

import java.util.List;

public record CatalogoRecepcionResponse(
        Long sucursalActivaId,
        List<SucursalRecepcionCatalogoResponse> sucursales,
        List<ServicioRecepcionCatalogoResponse> servicios,
        List<OpcionRecepcionResponse> estadosCita,
        String estadoCitaPendiente,
        String estadoCitaConfirmada,
        List<String> estadosCitaFinalizables,
        List<String> estadosCitaCancelables,
        List<OpcionRecepcionResponse> estadosEspera,
        String estadoEsperaPendiente,
        String estadoEsperaNotificada
) {
    public record OpcionRecepcionResponse(String codigo, String etiqueta) {
    }
}
