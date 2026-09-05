package com.techprotech.agenda.modulos.autenticacion.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public record RespuestaTokenJwt(
        String tokenAcceso,
        String tokenActualizacion,
        String tipoToken,
        Long usuarioId,
        Long empresaId,
        String empresaSlug,
        String empresaNombre,
        boolean correoVerificado,
        List<String> roles,
        List<String> permisos,
        List<Long> sucursalesPermitidas
) {
    @Override
    @JsonIgnore
    public String tokenActualizacion() {
        return tokenActualizacion;
    }
}
