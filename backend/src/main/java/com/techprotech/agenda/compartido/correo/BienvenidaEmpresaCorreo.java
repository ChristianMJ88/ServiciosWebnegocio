package com.techprotech.agenda.compartido.correo;

public record BienvenidaEmpresaCorreo(
        Long empresaId,
        String nombreEmpresa,
        String nombreAdministrador,
        String correoAdministrador,
        String slug
) {
}
