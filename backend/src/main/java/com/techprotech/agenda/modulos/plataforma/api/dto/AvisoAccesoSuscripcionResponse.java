package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.time.LocalDateTime;

public record AvisoAccesoSuscripcionResponse(boolean bloqueado, boolean advertencia,
        String titulo, String mensaje, LocalDateTime graciaHasta, boolean puedeRegularizar) {}
