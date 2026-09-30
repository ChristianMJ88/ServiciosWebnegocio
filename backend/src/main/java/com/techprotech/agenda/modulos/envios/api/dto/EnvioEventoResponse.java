package com.techprotech.agenda.modulos.envios.api.dto;

import java.time.LocalDateTime;

public record EnvioEventoResponse(
        Long id,
        String tipo,
        String estado,
        String descripcion,
        LocalDateTime creadoEn
) {}
