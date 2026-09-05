package com.techprotech.agenda.modulos.contactos.api.dto;

public record ResumenContactosResponse(
        long total,
        long nuevos,
        long enProceso,
        long atendidos,
        SolicitudContactoAdminResponse ultimoNuevo
) {
}
