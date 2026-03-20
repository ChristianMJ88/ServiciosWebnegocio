package com.techprotech.agenda.modulos.disponibilidad.aplicacion;

public record FranjaDisponibleResponse(
        String inicio,
        String fin,
        Long prestadorId,
        Long servicioId,
        Long sucursalId
) {
}

