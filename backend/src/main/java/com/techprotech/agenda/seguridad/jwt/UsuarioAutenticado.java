package com.techprotech.agenda.seguridad.jwt;

import java.util.List;

public record UsuarioAutenticado(
        String correo,
        Long usuarioId,
        Long empresaId,
        List<String> roles
) {
}

