package com.techprotech.agenda.modulos.envios.api.dto;

public record ModoCredencialEnvioResponse(
        String codigo,
        String nombre,
        String descripcion,
        boolean disponible
) {}
