package com.techprotech.agenda.modulos.autenticacion.social;

public record PerfilRegistroSocial(
        String proveedor,
        String subject,
        String correo,
        String nombre
) {
}
