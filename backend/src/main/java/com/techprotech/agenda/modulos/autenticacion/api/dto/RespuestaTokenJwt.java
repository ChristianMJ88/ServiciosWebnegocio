package com.techprotech.agenda.modulos.autenticacion.api.dto;

import java.util.List;

public record RespuestaTokenJwt(
        String tokenAcceso,
        String tokenActualizacion,
        String tipoToken,
        Long usuarioId,
        Long empresaId,
        List<String> roles
) {
}

