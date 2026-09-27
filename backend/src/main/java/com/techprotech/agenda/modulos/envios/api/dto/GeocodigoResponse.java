package com.techprotech.agenda.modulos.envios.api.dto;

public record GeocodigoResponse(
        boolean valido, String codigoPostal, String municipio, String estado, String pais,
        String latitud, String longitud
) {}
