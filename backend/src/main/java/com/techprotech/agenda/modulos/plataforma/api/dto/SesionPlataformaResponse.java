package com.techprotech.agenda.modulos.plataforma.api.dto;

import java.util.List;

public record SesionPlataformaResponse(
        String tokenAcceso,
        String tipoToken,
        Long usuarioId,
        String nombre,
        String correo,
        List<String> roles,
        List<String> permisos) {
}
