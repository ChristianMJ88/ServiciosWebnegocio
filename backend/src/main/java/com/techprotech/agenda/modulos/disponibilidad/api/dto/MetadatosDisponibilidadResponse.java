package com.techprotech.agenda.modulos.disponibilidad.api.dto;

import java.util.List;

public record MetadatosDisponibilidadResponse(
        List<OpcionTextoResponse> tiposSujeto,
        List<OpcionNumeroResponse> diasSemana,
        List<OpcionTextoResponse> tiposBloqueo,
        int intervaloMinimoMinutos
) {
    public record OpcionTextoResponse(String valor, String etiqueta) {
    }

    public record OpcionNumeroResponse(int valor, String etiqueta) {
    }
}
